<html>
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1, shrink-to-fit=no">
    <title>callback</title>
<body onload="document.callback.submit()">
<form action="${redirect_uri}" name="callback" autocomplete="off" method="post">
    <#list params as name, value>
    <input type="hidden" name="${name}" value="${value}"/>
    </#list>
</form>
</body>
</html>
