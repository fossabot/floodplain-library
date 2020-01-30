package com.dexels.navajo.adapters.stream;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import javax.sql.DataSource;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.dexels.immutable.api.ImmutableMessage;
import com.dexels.immutable.factory.ImmutableFactory;
import com.dexels.navajo.document.Message;
import com.dexels.navajo.document.Operand;
import com.dexels.navajo.document.Property;
import com.dexels.navajo.document.stream.StreamDocument;
import com.dexels.navajo.parser.Expression;
import com.dexels.navajo.script.api.SystemException;
import com.mysql.cj.jdbc.MysqlDataSourceFactory;
import com.mysql.cj.jdbc.MysqlXADataSource;

import io.reactivex.Single;
import io.reactivex.functions.BiFunction;
import io.reactivex.schedulers.Schedulers;

public class TestSQL {

	
	private static final Logger logger = LoggerFactory.getLogger(TestSQL.class);

	@Before
	public void setup() {
	}
	@Test(timeout=15000) @Ignore // at least until we have a docker based integration test.
	public void testSQL() {
		AtomicInteger count = new AtomicInteger();
		SQL.query("dummy", "tenant", "select * from ORGANIZATION WHERE ROWNUM <50")
			.map(msg->msg.without(Arrays.asList("SHORTNAME,UPDATEBY,REMARKS".split(","))))
			.flatMapSingle(e->getOrganizationAttributes(e))
			.map(e->rename("ORGANIZATIONID","ID").apply(e))
			.flatMap(msg->StreamDocument.replicationMessageToStreamEvents("Organization", msg, false))
			.compose(StreamDocument.inArray("Organization"))
			.lift(StreamDocument.serialize())
			.blockingForEach(e->count.incrementAndGet());
		logger.info("Total: {}",count.get());
	}

	public Single<ImmutableMessage> getOrganizationAttributes(ImmutableMessage msg) {
		return SQL.query("dummy", "tenant", "select * from ORGANIZATIONATTRIBUTE WHERE ORGANIZATIONID = ?", Operand.ofString((String)msg.value("ORGANIZATIONID").orElse("")))
			.observeOn(Schedulers.io())
			.subscribeOn(Schedulers.io())
			.reduce(msg, set("[ATTRIBNAME]", "[ATTRIBVALUE]"));
	}

	public static ImmutableMessage empty() {
		return ImmutableFactory.create(Collections.emptyMap(), Collections.emptyMap());
	}
	
	
	public BiFunction<ImmutableMessage,ImmutableMessage,ImmutableMessage> set(String... params) {
		return (reduc,item)->{
			String keyExpression = params[0];
			String valueExpression = params[1];
			String key = (String)evaluate(keyExpression,item).value;
			Operand evaluated = evaluate(valueExpression, item);
			Object value = evaluated.value;
			String valueType = evaluated.type;
			return reduc.with(key,value,valueType);
		};
	}
	
	

	private Operand evaluate(String valueExpression, ImmutableMessage m) throws SystemException {
		return Expression.evaluate(valueExpression, null, null, null,null,null,null,null,Optional.of(m),Optional.empty());
	}
	
	public Message getMessage(String prefix, Message core) {
		return core;
	}

	public Function<Message,Message> delete(String key) {
		return in->{
			Property p = in.getProperty(key);
			if(p!=null) {
				in.removeProperty(p);
			}
			return in;
		};
	}

	public Function<ImmutableMessage,ImmutableMessage> rename(String key, String to) {
		return in->{
			Optional<Object> value = in.value(key);
			if(!value.isPresent()) {
				return in;
			}
			String type = in.columnType(key);
			return in.without(key)
					.with(key, value.get(), type);
		};
	}
	
	public static DataSource resolveDataSource(String dataSourceName, String tenant) {
	    MysqlXADataSource mysqlXADataSource = new MysqlXADataSource();
	    mysqlXADataSource.setURL("jdbc:mysql://localhost/competition");
	    mysqlXADataSource.setUser("username");
	    mysqlXADataSource.setPassword("password");
	    return mysqlXADataSource;
	}
}