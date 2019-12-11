package com.dexels.server.mgmt.status;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;

import com.dexels.server.mgmt.api.ServerHealthCheck;

import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;

@Component(name="dexels.osgi.unresolvedbundlecheck",configurationPolicy=ConfigurationPolicy.REQUIRE)
public class UnresolvedBundleCheck implements ServerHealthCheck {
	private BundleContext _bundleContext;

	private List<String> currentIssues = new ArrayList<>();

	private Disposable updater = null;

	private int initial = 0;
	private int interval = 0;

	@Activate
	protected synchronized void activate(BundleContext bundleContext, Map<String,Object> settings) {
		_bundleContext = bundleContext;
		interval = tryToParseInteger(settings.get("interval")).orElse(1000);
		initial = tryToParseInteger(settings.get("initial")).orElse(1000);
		updater = Observable.interval(initial,interval, TimeUnit.MILLISECONDS).subscribe(l->update());
	}
	
	private static Optional<Integer> tryToParseInteger(Object thing) {
		try {
			return Optional.ofNullable(Integer.parseInt((String) thing));
		} catch (NumberFormatException e) {
			return Optional.empty();
		}
	}
	@Deactivate
	public synchronized void deactivate() {
		if(updater!=null) {
			updater.dispose();
		}
	}
	@Override
	public synchronized boolean isOk() {
		return this.currentIssues.isEmpty();
	}

	@Override
	public synchronized String getDescription() {
		return this.currentIssues.stream().collect(Collectors.joining("\n"));
	}
	
	private synchronized void update() {
		this.currentIssues.clear();
		this.currentIssues.addAll(installedBundles());
	}


	public List<String> installedBundles() {
		return Observable.fromArray(this._bundleContext.getBundles())
			.filter(b->b.getState()==Bundle.INSTALLED)
			.map(b->"Bundle failed to resolve: "+b.getSymbolicName()+"\n")
			.collect(()->new ArrayList<String>(), (list,e)->list.add(e))
			.blockingGet();
	}

}
