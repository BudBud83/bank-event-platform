package com.bankevent.api;

import com.bankevent.account.AccountService;
import com.bankevent.api.dto.AccountResponse;
import com.bankevent.api.dto.CreateAccountRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * HTTP façade for accounts.
 *
 * <p>{@code @RestController} = {@code @Controller} + {@code @ResponseBody}:
 * return values are serialized to JSON. {@code @RequestMapping} sets the
 * URL prefix. This class should stay thin: validate, call the service, return.
 */
@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    private final AccountService accounts;

    public AccountController(AccountService accounts) {
        this.accounts = accounts;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AccountResponse open(@Valid @RequestBody CreateAccountRequest request) {
        return accounts.open(request);
    }

    @GetMapping
    public List<AccountResponse> list() {
        return accounts.list();
    }

    @GetMapping("/{accountId}")
    public AccountResponse get(@PathVariable String accountId) {
        return accounts.get(accountId);
    }
}
