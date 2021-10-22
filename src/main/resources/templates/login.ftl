<#import "main.ftl" as layout />

<@layout.mainLayout title="mock-oauth2-server" description="Just a mock login">
<div class="container">
    <section class="header">
        <h2 class="title">Mock OAuth2 Server Sign-in</h2>
    </section>
    <div class="presets-section">
        <script lang="javascript">
            function setUsername(username) {
                document.getElementById('username').value = username;
            }

            function setClaims(claims) {
                document.getElementById('claims').value = claims
            }

            const presets = {
                <#list presets as preset>
                '${preset.name}' : {
                    'username' : '${preset.username}',
                    'claims' : `${preset.claimsAsString}`
                },
                </#list>
            }
        </script>
        <div>
            <#list presets as preset>
                <button onClick="setUsername(presets['${preset.name}'].username); setClaims(presets['${preset.name}'].claims);">${preset.name}</button>
            </#list>
            <button onClick="setUsername(''); setClaims('');">Clear</button>
        </div>
    </div>
    <div class="docs-section" id="sign-in">
        <div class="row">
            <div class="three columns">&nbsp;</div>
            <div class="six columns">
                <form method="post">
                    <label>
                        <input class="u-full-width" required type="text" id="username" name="username"
                               placeholder="Enter any user/subject"
                               autofocus="on">
                    </label>
                    <label>
                        <textarea class="u-full-width claims" id="claims" name="claims" rows="15"
                               placeholder="Optional claims JSON value, example:
{
  &quot;acr&quot;: &quot;reference&quot;
}" autofocus="on"></textarea>
                    </label>
                    <input class="button-primary" type="submit" value="Sign-in">
                </form>
            </div>
            <div class="three columns">&nbsp;</div>
        </div>
    </div>
    <div class="docs-section">

        <div class="row">
            <div class="three columns">&nbsp;</div>
            <div class="six columns">
                <h6 class="docs-header">Authorization Request</h6>
                <#list query as propName, propValue>
                    <div style="text-align: left; color:grey;"><strong>${propName}</strong> = ${propValue}</div>
                </#list>
            </div>
            <div class="three columns">&nbsp;</div>
        </div>
    </div>
</div>
</@layout.mainLayout>
