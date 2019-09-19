/*
 * Copyright 2017-2020 the original author or authors.
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

package org.springframework.data.couchbase.repository.support;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.reactivestreams.Publisher;
import org.springframework.data.couchbase.core.CouchbaseOperations;
import org.springframework.data.couchbase.core.ReactiveCouchbaseOperations;
import org.springframework.data.couchbase.core.query.Query;
import org.springframework.data.couchbase.repository.ReactiveCouchbaseRepository;
import org.springframework.data.couchbase.repository.query.CouchbaseEntityInformation;
import org.springframework.data.domain.Sort;
import org.springframework.data.util.Streamable;
import org.springframework.util.Assert;

/**
 * Reactive repository base implementation for Couchbase.
 *
 * @author Subhashni Balakrishnan
 * @author Mark Paluch
 * @author Christoph Strobl
 * @author David Kelly
 * @author Douglas Six
 * @since 3.0
 */
public class SimpleReactiveCouchbaseRepository<T, ID> implements ReactiveCouchbaseRepository<T, ID> {

	/**
	 * Holds the reference to the {@link CouchbaseOperations}.
	 */
	private final ReactiveCouchbaseOperations operations;

	/**
	 * Contains information about the entity being used in this repository.
	 */
	private final CouchbaseEntityInformation<T, String> entityInformation;

	/**
	 * Create a new Repository.
	 *
	 * @param metadata the Metadata for the entity.
	 * @param operations the reference to the reactive template used.
	 */
	public SimpleReactiveCouchbaseRepository(final CouchbaseEntityInformation<T, String> metadata,
			final ReactiveCouchbaseOperations operations) {
		Assert.notNull(operations, "RxJavaCouchbaseOperations must not be null!");
		Assert.notNull(metadata, "CouchbaseEntityInformation must not be null!");

		this.entityInformation = metadata;
		this.operations = operations;
	}

	@SuppressWarnings("unchecked")
	public <S extends T> Mono<S> save(final S entity) {
		Assert.notNull(entity, "Entity must not be null!");
		return (Mono<S>) operations.upsertById(entityInformation.getJavaType()).one(entity);
	}

	@Override
	public Flux<T> findAll(final Sort sort) {
		return findAll(new Query().with(sort));
	}

	@SuppressWarnings("unchecked")
	@Override
	public <S extends T> Flux<S> saveAll(final Iterable<S> entities) {
		Assert.notNull(entities, "The given Iterable of entities must not be null!");
		return (Flux<S>) operations.upsertById(entityInformation.getJavaType()).all(Streamable.of(entities).toList());
	}

	@SuppressWarnings("unchecked")
	@Override
	public <S extends T> Flux<S> saveAll(final Publisher<S> entityStream) {
		Assert.notNull(entityStream, "The given Iterable of entities must not be null!");
		return Flux.from(entityStream).flatMap(this::save);
	}

	@SuppressWarnings("unchecked")
	@Override
	public Mono<T> findById(final ID id) {
		return operations.findById(entityInformation.getJavaType()).one(id.toString());
	}

	@SuppressWarnings("unchecked")
	@Override
	public Mono<T> findById(final Publisher<ID> publisher) {
		Assert.notNull(publisher, "The given Publisher must not be null!");
		return Mono.from(publisher).flatMap(this::findById);
	}

	@SuppressWarnings("unchecked")
	@Override
	public Mono<Boolean> existsById(final ID id) {
		Assert.notNull(id, "The given id must not be null!");
		return operations.existsById().one(id.toString());
	}

	@SuppressWarnings("unchecked")
	@Override
	public Mono<Boolean> existsById(final Publisher<ID> publisher) {
		Assert.notNull(publisher, "The given Publisher must not be null!");
		return Mono.from(publisher).flatMap(this::existsById);
	}

	@SuppressWarnings("unchecked")
	@Override
	public Flux<T> findAll() {
		return findAll(new Query());
	}

	@SuppressWarnings("unchecked")
	@Override
	public Flux<T> findAllById(final Iterable<ID> ids) {
		Assert.notNull(ids, "The given Iterable of ids must not be null!");
		List<String> convertedIds = Streamable.of(ids).stream().map(Objects::toString).collect(Collectors.toList());
		return (Flux<T>) operations.findById(entityInformation.getJavaType()).all(convertedIds);
	}

	@SuppressWarnings("unchecked")
	@Override
	public Flux<T> findAllById(final Publisher<ID> entityStream) {
		Assert.notNull(entityStream, "The given entityStream must not be null!");
		return Flux.from(entityStream).flatMap(this::findById);
	}

	@SuppressWarnings("unchecked")
	@Override
	public Mono<Void> deleteById(final ID id) {
		return operations.removeById().one(id.toString()).then();
	}

	@Override
	public Mono<Void> deleteById(final Publisher<ID> publisher) {
		Assert.notNull(publisher, "The given id must not be null!");
		return Mono.from(publisher).flatMap(this::deleteById);
	}

	@SuppressWarnings("unchecked")
	@Override
	public Mono<Void> delete(final T entity) {
		Assert.notNull(entity, "Entity must not be null!");
		return operations.removeById().one(entityInformation.getId(entity)).then();
	}

	@SuppressWarnings("unchecked")
	@Override
	public Mono<Void> deleteAll(final Iterable<? extends T> entities) {
		return operations.removeById().all(Streamable.of(entities).map(entityInformation::getId).toList()).then();
	}

	@Override
	public Mono<Void> deleteAll(final Publisher<? extends T> entityStream) {
		Assert.notNull(entityStream, "The given publisher of entities must not be null!");
		return Flux.from(entityStream).flatMap(this::delete).single();
	}

	@SuppressWarnings("unchecked")
	@Override
	public Mono<Long> count() {
		return operations.findByQuery(entityInformation.getJavaType()).count();
	}

	@SuppressWarnings("unchecked")
	@Override
	public Mono<Void> deleteAll() {
		return operations.removeByQuery(entityInformation.getJavaType()).all().then();
	}

	/**
	 * Returns the information for the underlying template.
	 *
	 * @return the underlying entity information.
	 */
	protected CouchbaseEntityInformation<T, String> getEntityInformation() {
		return entityInformation;
	}

	@Override
	public ReactiveCouchbaseOperations getReactiveCouchbaseOperations() {
		return operations;
	}

	private Flux<T> findAll(final Query query) {
		return operations.findByQuery(entityInformation.getJavaType()).matching(query).all();
	}

}
