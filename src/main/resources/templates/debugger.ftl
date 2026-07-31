<#import "main.ftl" as layout />

<@layout.mainLayout title="mock-oauth2-server debugger" description="Just a mock oauth2 client">
    <div class="container">
        <section class="header">
            <h2 class="title">OAuth2 Client Debugger</h2>
        </section>

        <div class="docs-section" id="openid">
            <h6 class="docs-header">OpenID Connect</h6>
            <p>Insert your parameters here to run the Authorization Code Flow against this server</p>
            <div>
                <form method="post">
                    <label for="authorize_url">Authorization Endpoint</label>
                    <input class="u-full-width" type="text" name="authorize_url" value="${url}" readonly>
                    <label for="token_url">Token Endpoint</label>
                    <input class="u-full-width" type="text" name="token_url" value="${token_url}" readonly>
                    <label for="client_auth_method">Client Authentication Method</label>
                    <input class="u-full-width" type="text" placeholder="Client auth method" name="client_auth_method"
                           value="${client_auth_method}">
                    <label for="client_id">Client Id</label>
                    <input class="u-full-width" type="text" placeholder="Any client_id, this server does not require registration" name="client_id"
                           value="${query.client_id}">
                    <label for="client_secret">Client Secret (used to acquire token when callback is received)</label>
                    <input class="u-full-width" type="text" placeholder="Any client_secret, this server does not require registration"
                           name="client_secret" value="someSecret">
                    <label for="scope">Scope</label>
                    <input class="u-full-width" type="text"
                           placeholder="Scopes space separated, should include 'openid'" name="scope"
                           value="${query.scope}">
                    <label for="response_type">Response Type</label>
                    <input class="u-full-width" type="text" placeholder="Choice between [code, token, id_token]"
                           name="response_type" value="${query.response_type}">
                    <label for="response_mode">Response Mode</label>
                    <input class="u-full-width" type="text" placeholder="Choice between [query, fragment, form_post]"
                           name="response_mode" value="${query.response_mode}">
                    <label for="state">State</label>
                    <input class="u-full-width" type="text" placeholder="A state value to protect against CSRF"
                           name="state" value="${query.state}">
                    <label for="nonce">Nonce</label>
                    <input class="u-full-width" type="text"
                           placeholder="A nonce value to include in token (for replay protection)" name="nonce"
                           value="${query.nonce}">
                    <label for="redirect_uri">Redirect URI (callback)</label>
                    <p style="margin-bottom: 0.5rem; color: grey">The debugger's own callback, which completes the
                        flow and displays the token response</p>
                    <input class="u-full-width" type="text" name="redirect_uri" value="${query.redirect_uri}" readonly>
                    <input class="button-primary" type="submit" value="Get a token">
                </form>
            </div>
        </div>

        <div class="docs-section" id="forms">
            <h6 class="docs-header">more debugging options</h6>
            <p>More options will be added shortly</p>
        </div>
    </div>
</@layout.mainLayout>
