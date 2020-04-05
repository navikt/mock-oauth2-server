<#import "main.ftl" as layout />

<@layout.mainLayout title="mock-oauth2-server debugger" description="Just a mock oauth2 client">
    <div class="container">
        <section class="header">
            <h2 class="title">OAuth2 Client Debugger</h2>
        </section>

        <div class="docs-section" id="openid">
            <h3 class="docs-header">OpenID Connect Callback</h3>
            <p>Inspect callback parameters and the actual token response</p>
            <div>
                <label for="token_request">Token Request</label>
                <pre id="token_request"><code>${token_request}</code></pre>
                <label>Token Response</label>
                <pre><code>${token_response}</code></pre>
            </div>
        </div>

        <div class="docs-section" id="forms">
            <h6 class="docs-header">more debugging options</h6>
            <p>More options will be added shortly</p>
        </div>
    </div>
</@layout.mainLayout>
