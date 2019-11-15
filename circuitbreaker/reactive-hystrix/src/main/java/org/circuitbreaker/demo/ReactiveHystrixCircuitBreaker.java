/*
 * Copyright 2013-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.circuitbreaker.demo;

import java.util.function.Function;

import org.reactivestreams.Publisher;

import com.netflix.hystrix.HystrixCommandGroupKey;
import com.netflix.hystrix.HystrixCommandProperties;
import com.netflix.hystrix.HystrixObservableCommand;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import rx.Observable;
import rx.RxReactiveStreams;
import rx.Subscription;


public class ReactiveHystrixCircuitBreaker /*implements CircuitBreaker*/ {

	private final String id;
	private final HystrixCommandProperties.Setter commandPropertiesSetter;

	public ReactiveHystrixCircuitBreaker(String id, HystrixCommandProperties.Setter commandPropertiesSetter) {
		this.id = id;
		this.commandPropertiesSetter = commandPropertiesSetter;
	}


	//	@Override
	public <T> Mono<T> run(Mono<T> toRun) {
		return run(toRun, null);
	}

	//	@Override
	public <T> Mono<T> run(Mono<T> toRun, Function<Throwable, Mono<T>> fallback) {
		final HystrixObservableCommand<T> command = createCommand(toRun, fallback);

		return Mono.create(s -> {
			final Subscription sub = command.toObservable().subscribe(s::success, s::error, s::success);
			s.onCancel(sub::unsubscribe);
		});
	}

	public <T> Flux<T> run(Flux<T> toRun) {
		return run(toRun , null);
	}

	public <T> Flux<T> run(Flux<T> toRun, Function<Throwable, Flux<T>> fallback) {
		final HystrixObservableCommand<T> command = createCommand(toRun, fallback);

		return Flux.create(s -> {
			final Subscription sub = command.toObservable().subscribe(s::next, s::error, s::complete);
			s.onCancel(sub::unsubscribe);
		});
	}

	private <T> HystrixObservableCommand<T> createCommand(Publisher<T> toRun, Function fallback) {
		final HystrixObservableCommand.Setter setter = HystrixObservableCommand.Setter.withGroupKey(HystrixCommandGroupKey.Factory.asKey(id))
				.andCommandPropertiesDefaults(commandPropertiesSetter);
		final HystrixObservableCommand<T> command = new HystrixObservableCommand<T>(setter) {
			@Override
			protected Observable<T> construct() {
				return RxReactiveStreams.toObservable(toRun);
			}

			@Override
			protected Observable<T> resumeWithFallback() {
				if(fallback == null) {
					super.resumeWithFallback();
				}
				return RxReactiveStreams.toObservable((Publisher)fallback.apply(this.getExecutionException()));
			}
		};
		return command;
	}
}
