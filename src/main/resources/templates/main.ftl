<#macro mainLayout title="" description="">
    <!DOCTYPE html>
    <html lang="en">
    <head>
        <meta charset="utf-8">
        <title>${title} | ${description}</title>
        <meta name="description" content="">
        <meta name="author" content="">
        <meta name="viewport" content="width=device-width, initial-scale=1">
        <link href="//fonts.googleapis.com/css?family=Raleway:400,300,600" rel="stylesheet" type="text/css">
        <style type="text/css">
            <#include "css/normalize.css">
        </style>
        <style type="text/css">
            <#include "css/skeleton.css">
        </style>
        <style type="text/css">
            <#include "css/custom.css">
        </style>
    </head>
    <body class="code-snippets-visible">
    <#nested />
    </body>
    </html>
</#macro>
