package io.floodplain.kafka.webapi;

import io.floodplain.pubsub.rx2.api.PersistentPublisher;
import io.floodplain.pubsub.rx2.api.PersistentSubscriber;
import io.floodplain.pubsub.rx2.api.TopicPublisher;
import io.floodplain.replication.api.ReplicationMessage;
import io.floodplain.replication.api.ReplicationMessageParser;
import io.floodplain.replication.impl.protobuf.FallbackReplicationMessageParser;
import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Action;
import io.reactivex.functions.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.AsyncContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class KafkaDownloadMongoServlet extends HttpServlet {

    private static final long serialVersionUID = 9025834882284006898L;
    private static final Logger logger = LoggerFactory.getLogger(KafkaDownloadMongoServlet.class);

    private PersistentSubscriber persistentSubscriber;
    private PersistentPublisher publisher;
    private Set<Disposable> running = new HashSet<>();

    public void activate() {

    }

    public void deactivate() {
        running.forEach(e -> e.dispose());
    }

    public void setPersistentSubscriber(PersistentSubscriber persistenSubscriber) {
        this.persistentSubscriber = persistenSubscriber;
    }

    public void clearPersistentSubscriber(PersistentSubscriber persistenSubscriber) {
        this.persistentSubscriber = null;
    }

    public void setTopicPublisher(PersistentPublisher publisher) {
        this.publisher = publisher;
    }


    public void clearTopicPublisher(TopicPublisher publisher) {
        this.publisher = null;
    }


    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String host = req.getParameter("host");
        String port = req.getParameter("port") == null ? "27017" : req.getParameter("port");
        String database = req.getParameter("database");
        String collection = req.getParameter("collection");
        String topic = req.getParameter("topic");

        resp.setCharacterEncoding("UTF-8");
        resp.setContentType("text/plain");
        AsyncContext ac = req.startAsync();
        ac.setTimeout(8000000);
        Disposable d = downloadTopicToMongoDb(host, Integer.parseInt(port), database, collection, this.persistentSubscriber, this.publisher, topic, e -> true)
                .flatMapCompletable(e -> e)
                .doOnComplete(() -> {
                    resp.getWriter().write("done!");
                    resp.getWriter().close();
                })
                .subscribe();
        running.add(d);
    }

    private static Flowable<Completable> downloadTopicToMongoDb(String host, int port, String database, String collection, PersistentSubscriber kts, PersistentPublisher pub, String topic,
                                                                Predicate<ReplicationMessage> filter) throws IOException {
        System.setProperty(ReplicationMessage.PRETTY_JSON, "true");

        ReplicationMessageParser parser = new FallbackReplicationMessageParser(true);

        Map<Integer, Long> offsetMap = kts.partitionOffsets(topic);
        Map<Integer, Long> offsetMapInc = new HashMap<Integer, Long>();
        offsetMap.entrySet().forEach(e -> {
            offsetMapInc.put(e.getKey(), e.getValue() - 1);
        });
        String toTag = kts.encodeTopicTag(offsetMapInc);
        // TODO multiple partitions
        String fromTag = "0:0";
        AtomicLong messageCount = new AtomicLong();
        AtomicLong writtenDataCount = new AtomicLong();
        AtomicLong writtenMessageCount = new AtomicLong();

        final Disposable d = Flowable.interval(2, TimeUnit.SECONDS)
                .doOnNext(e -> logger.info("In progress. MessageCount: " + messageCount.get()
                        + " writtenMessageCount: " + writtenMessageCount + " written data: " + writtenDataCount.get()))
                .doOnTerminate(() -> logger.info("Progress complete")).subscribe();
        final String generatedConsumerGroup = UUID.randomUUID().toString();
        Action onTerminate = () -> {
            d.dispose();
            pub.deleteGroups(Arrays.asList(new String[]{generatedConsumerGroup}));
        };
        // TODO rebuild mongo direct here?
        return Flowable.empty();
//		return Flowable.fromPublisher(kts.subscribeSingleRange(topic, generatedConsumerGroup, fromTag, toTag))
//				.subscribeOn(Schedulers.io())
//				.observeOn(Schedulers.io())
//				.concatMapIterable(e -> e)
//				.doOnNext(m -> messageCount.incrementAndGet()).retry(5)
//				.map(e->parser.parseBytes(e))
//				.filter(filter)
//				.compose(MongoDirectSink.createMongoCollection(database,host,""+port,collection))
//				.doOnTerminate(onTerminate)
//				.doOnCancel(onTerminate);
    }

}
