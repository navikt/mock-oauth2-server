<#import "main.ftl" as layout />

<@layout.mainLayout title="mock-oauth2-server debugger" description="Just a mock oauth2 client">
    <div class="container">
        <section class="header">
            <h2 class="title">OAuth2 Client Debugger</h2>
        </section>

        <div class="docs-section" id="openid">
            <h3 class="docs-header">Something went wrong.</h3>
            <p>Could be expired session? Please try again using the debugger form - <a href='${debugger_url}'>${debugger_url}</a></p>

            <#if error_parameters?has_content>
            <div>
                <label for="error_parameters">Authorization error response</label>
                <pre id="error_parameters"><code><#list error_parameters as name, value>${name}=${value}
</#list></code></pre>
            </div>
            </#if>
            <div>
                <label for="stacktrace">Stacktrace</label>
                <pre id="stacktrace"><code>${stacktrace}</code></pre>
            </div>
        </div>

        <div class="docs-section" id="forms">
            <h6 class="docs-header">more debugging options</h6>
            <p>More options will be added shortly</p>
        </div>
    </div>
</@layout.mainLayout>
