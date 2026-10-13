package com.bankevent.account;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data generates the SQL for us from the method names and the
 * {@link JpaRepository} contract ({@code save}, {@code findById}, {@code findAll}).
 *
 * <p>This is the only way services should touch the {@code accounts} table.
 */
public interface AccountRepository extends JpaRepository<Account, String> {
}
