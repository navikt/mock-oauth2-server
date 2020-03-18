<#macro mainLayout title="" description="">
<!DOCTYPE html>
<#--noinspection HtmlRequiredLangAttribute-->
<html>
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1, shrink-to-fit=no">
    <title>${title} | ${description}</title>
    <style type="text/css">
        <#--noinspection CssUnknownTarget-->
        @import url(https://fonts.googleapis.com/css?family=Roboto:400,100);

        body {
            font-family: 'Roboto', sans-serif;
            margin-top: 25px;
            margin-bottom: 25px;
        }

        .login-card {
            padding: 0px 40px 10px;
            width: 274px;
            background-color: #F7F7F7;
            margin: 0 auto 10px;
            border-radius: 2px;
            box-shadow: 0 2px 2px rgba(0, 0, 0, 0.3);
            overflow: hidden;
        }

        .login-card h1 {
            font-weight: 100;
            text-align: center;
            font-size: 2.3em;
        }

        .login-card [type=submit] {
            width: 100%;
            display: block;
            margin-bottom: 10px;
            position: relative;
        }

        .login-card input[type=text], input[type=email], input[type=password] {
            height: 44px;
            font-size: 16px;
            width: 100%;
            margin-bottom: 10px;
            -webkit-appearance: none;
            background: #fff;
            border: 1px solid #d9d9d9;
            border-top: 1px solid #c0c0c0;
            padding: 0 8px;
            box-sizing: border-box;
            -moz-box-sizing: border-box;
        }

        .login-card input[type=text]:hover, input[type=email]:hover, input[type=password]:hover {
            border: 1px solid #b9b9b9;
            border-top: 1px solid #a0a0a0;
            -moz-box-shadow: inset 0 1px 2px rgba(0, 0, 0, 0.1);
            -webkit-box-shadow: inset 0 1px 2px rgba(0, 0, 0, 0.1);
            box-shadow: inset 0 1px 2px rgba(0, 0, 0, 0.1);
        }

        .login {
            text-align: center;
            font-size: 14px;
            font-family: 'Arial', sans-serif;
            font-weight: 700;
            height: 36px;
            padding: 0 8px;
        }

        .login-submit {
            border: 0;
            color: #fff;
            text-shadow: 0 1px rgba(0, 0, 0, 0.1);
            background-color: #4d90fe;
        }

        .login-submit:hover {
            border: 0;
            text-shadow: 0 1px rgba(0, 0, 0, 0.3);
            background-color: #357ae8;
        }

        .login-card a {
            text-decoration: none;
            color: #666;
            font-weight: 400;
            text-align: center;
            display: inline-block;
            opacity: 0.6;
            transition: opacity ease 0.5s;
        }

        .login-card a:hover {
            opacity: 1;
        }

        .login-help {
            width: 100%;
            text-align: center;
            font-size: 12px;
        }

        .login-client-image img {
            margin-bottom: 20px;
        }

        .login-card input[type=checkbox] {
            margin-bottom: 10px;
        }

        .login-card label {
            color: #999;
            font-size: 16px;
        }

        .grant-debug {
            font-family: Fixed, monospace;
            width: 100%;
            text-align: center;
            font-size: 12px;
            color: #999;
            padding-top: 10px;
        }

        button {
            cursor: pointer;
        }
    </style>
</head>
<body>
    <#nested />
</body>
</html>
</#macro>
