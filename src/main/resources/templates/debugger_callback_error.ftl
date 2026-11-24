<#ftl output_format="HTML">
<#import "main.ftl" as layout />

<@layout.mainLayout title="mock-oauth2-server debugger" description="Just a mock oauth2 client">
    <div class="container">
        <section class="header">
            <h2 class="title">OAuth2 Client Debugger</h2>
        </section>

        <div class="docs-section" id="openid">
            <h3 class="docs-header">Authorization error response</h3>
            <p>The authorization request failed. Please try again using the debugger form - <a href="${debugger_url}">${debugger_url}</a></p>
            <div>
                <label for="error">error</label>
                <pre id="error"><code>${error}</code></pre>
                <#if error_description??>
                <label for="error_description">error_description</label>
                <pre id="error_description"><code>${error_description}</code></pre>
                </#if>
                <#if error_uri??>
                <label for="error_uri">error_uri</label>
                <pre id="error_uri"><code>${error_uri}</code></pre>
                </#if>
            </div>
        </div>
    </div>
</@layout.mainLayout>
