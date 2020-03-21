<#import "layout.ftl" as layout />

<@layout.mainLayout title="mock-oauth2-server" description="Just a mock login">
    <div class="login-card">
        <h1>Sign-in to the Mock OAuth2 Server</h1>
        <form autocomplete="off" method="post">
            <input type="hidden" name="uuid" value=""/>
            <label>
                <input required type="text" name="username" placeholder="Enter any user/subject" autofocus="on">
            </label>
            <button type="submit" class="login login-submit">Sign-in</button>
        </form>
        <div class="grant-debug">
            <h3>Debug info</h3>
            <#list query as propName, propValue>
                <div align="left"><strong>${propName}</strong> = ${propValue}</div>
            </#list>
        </div>
    </div>
</@layout.mainLayout>
