package com.pushkar.codereview.github.client.dto;

public class GithubInstallationDetailsResponse {

    private Long id;
    private Account account;

    public GithubInstallationDetailsResponse() {
    }

    public GithubInstallationDetailsResponse(Long id, String login, String type) {
        this.id = id;
        this.account = new Account(login, type);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Account getAccount() {
        return account;
    }

    public void setAccount(Account account) {
        this.account = account;
    }

    public String getAccountLogin() {
        return account != null ? account.getLogin() : null;
    }

    public String getAccountType() {
        return account != null ? account.getType() : null;
    }

    public static class Account {
        private String login;
        private String type;

        public Account() {
        }

        public Account(String login, String type) {
            this.login = login;
            this.type = type;
        }

        public String getLogin() {
            return login;
        }

        public void setLogin(String login) {
            this.login = login;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }
    }
}
