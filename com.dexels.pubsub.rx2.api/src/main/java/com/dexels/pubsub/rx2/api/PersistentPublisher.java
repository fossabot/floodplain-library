package com.dexels.pubsub.rx2.api;

import java.util.List;
import java.util.Map;

import org.osgi.annotation.versioning.ProviderType;

import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Observable;
import io.reactivex.Single;

@ProviderType
public interface PersistentPublisher extends TopicPublisher {
	public Observable<String> listConsumerGroups();
	public Single<Map<String,Long>> consumerGroupOffsets(String groupId);
	public Flowable<Map<String,String>> describeConsumerGroups(List<String> groups);
	public void describeTopic(String string);
	public Completable deleteGroups(List<String> groups);

}