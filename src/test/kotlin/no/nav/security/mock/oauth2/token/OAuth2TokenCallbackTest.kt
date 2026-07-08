package no.nav.security.mock.oauth2.token

import io.kotest.assertions.asClue
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.maps.shouldContainAll
import io.kotest.matchers.shouldBe
import no.nav.security.mock.oauth2.testutils.nimbusTokenRequest
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

internal class OAuth2TokenCallbackTest {
    private val clientId = "clientId"

    @Nested
    inner class RequestMappingTokenCallbacks {
        private val issuer1 =
            RequestMappingTokenCallback(
                issuerId = "issuer1",
                requestMappings =
                    listOf(
                        RequestMapping(
                            requestParam = "scope",
                            match = "scope1",
                            claims =
                                mapOf(
                                    "sub" to "subByScope1",
                                    "aud" to listOf("audByScope1"),
                                    "custom" to "custom1",
                                ),
                        ),
                        RequestMapping(
                            requestParam = "scope",
                            match = "scope2",
                            typeHeader = "JWT2",
                            claims =
                                mapOf(
                                    "sub" to "subByScope2",
                                    "aud" to listOf("audByScope2"),
                                    "custom" to "custom2",
                                ),
                        ),
                        RequestMapping(
                            requestParam = "audience",
                            match = "https://myapp.com/jwt/aud/.*",
                            claims =
                                mapOf(
                                    "sub" to "\${clientId}",
                                    "aud" to listOf("\${audience}"),
                                ),
                        ),
                        RequestMapping(
                            requestParam = "grant_type",
                            match = "authorization_code",
                            claims =
                                mapOf(
                                    "sub" to "defaultSub",
                                    "aud" to listOf("defaultAud"),
                                ),
                        ),
                        RequestMapping(
                            requestParam = "grant_type",
                            match = "*",
                            claims =
                                mapOf(
                                    "sub" to "\${clientId}",
                                    "aud" to listOf("defaultAud"),
                                ),
                        ),
                    ),
                tokenExpiry = 120,
            )

        @Test
        fun `token request with request params matching requestmapping should return specific claims from callback with default JWT type`() {
            val scopeShouldMatch = clientCredentialsRequest("scope" to "scope1")
            assertSoftly {
                issuer1.subject(scopeShouldMatch) shouldBe "subByScope1"
                issuer1.audience(scopeShouldMatch) shouldBe listOf("audByScope1")
                issuer1.tokenExpiry() shouldBe 120
                issuer1.addClaims(scopeShouldMatch) shouldContainAll mapOf("custom" to "custom1")
            }
        }

        @Test
        fun `token request with request params matching requestmapping should return specific claims from callback with non-default JWT type`() {
            val scopeShouldMatch = clientCredentialsRequest("scope" to "scope2")
            assertSoftly {
                issuer1.subject(scopeShouldMatch) shouldBe "subByScope2"
                issuer1.audience(scopeShouldMatch) shouldBe listOf("audByScope2")
                issuer1.tokenExpiry() shouldBe 120
                issuer1.typeHeader(scopeShouldMatch) shouldBe "JWT2"
            }
        }

        @Test
        fun `token request with request params matching wildcard requestmapping should return default claims from callback`() {
            val shouldMatchAllGrantTypes = authCodeRequest()
            assertSoftly {
                issuer1.subject(shouldMatchAllGrantTypes) shouldBe "defaultSub"
                issuer1.audience(shouldMatchAllGrantTypes) shouldBe listOf("defaultAud")
                issuer1.tokenExpiry() shouldBe 120
                issuer1.typeHeader(shouldMatchAllGrantTypes) shouldBe "JWT"
            }
        }

        @Test
        fun `token request with request params matching requestmapping should return specific claims from callback with sub set to ${clientId}`() {
            val grantTypeShouldMatch = clientCredentialsRequest()
            assertSoftly {
                issuer1.subject(grantTypeShouldMatch) shouldBe clientId
                issuer1.audience(grantTypeShouldMatch) shouldBe listOf("defaultAud")
                issuer1.tokenExpiry() shouldBe 120
                issuer1.typeHeader(grantTypeShouldMatch) shouldBe "JWT"
            }
        }

        @Test
        fun `token request with client_id via HTTP Basic auth should match requestmapping on client_id`() {
            val callback =
                RequestMappingTokenCallback(
                    issuerId = "issuer1",
                    requestMappings =
                        listOf(
                            RequestMapping(
                                requestParam = "client_id",
                                match = clientId,
                                claims = mapOf("sub" to "subByClientId", "aud" to listOf("audByClientId")),
                            ),
                        ),
                )
            val requestWithBasicAuth = clientCredentialsRequest()
            assertSoftly {
                callback.subject(requestWithBasicAuth) shouldBe "subByClientId"
                callback.audience(requestWithBasicAuth) shouldBe listOf("audByClientId")
            }
        }

        @Test
        fun `token request with client_id via HTTP Basic auth and wildcard requestmapping should match`() {
            val callback =
                RequestMappingTokenCallback(
                    issuerId = "issuer1",
                    requestMappings =
                        listOf(
                            RequestMapping(
                                requestParam = "client_id",
                                match = "*",
                                claims = mapOf("sub" to "wildcardSub"),
                            ),
                        ),
                )
            assertSoftly {
                callback.subject(clientCredentialsRequest()) shouldBe "wildcardSub"
            }
        }

        @Test
        fun `invalid regex in match skips regex evaluation without throwing, exact-string matching still applies`() {
            val callback =
                RequestMappingTokenCallback(
                    issuerId = "issuer1",
                    requestMappings =
                        listOf(
                            RequestMapping(
                                requestParam = "scope",
                                match = "[invalid(regex",
                                claims = mapOf("sub" to "shouldNotMatch"),
                            ),
                        ),
                )
            callback.addClaims(clientCredentialsRequest("scope" to "anything")) shouldBe emptyMap()
            callback.addClaims(clientCredentialsRequest("scope" to "[invalid(regex")) shouldContainAll mapOf("sub" to "shouldNotMatch")
        }

        @Test
        fun `token request with request params matching requestmapping should return specific claims from callback with audience`() {
            val grantTypeShouldMatch = clientCredentialsRequest("audience" to "https://myapp.com/jwt/aud/xxx")
            assertSoftly {
                issuer1.subject(grantTypeShouldMatch) shouldBe clientId
                issuer1.audience(grantTypeShouldMatch) shouldBe listOf("https://myapp.com/jwt/aud/xxx")
                issuer1.tokenExpiry() shouldBe 120
                issuer1.typeHeader(grantTypeShouldMatch) shouldBe "JWT"
            }
        }

        @Test
        fun `token request with custom parameters in token request should include claims with placeholder names`() {
            val request =
                clientCredentialsRequest(
                    "scope" to "testscope:something another:scope",
                    "mock_token_type" to "custom",
                )
            RequestMappingTokenCallback(
                issuerId = "issuer1",
                requestMappings =
                    listOf(
                        RequestMapping(
                            requestParam = "scope",
                            match = "testscope:.*",
                            claims =
                                mapOf(
                                    "sub" to "\${clientId}",
                                    "scope" to "\${scope}",
                                    "mock_token_type" to "\${mock_token_type}",
                                ),
                        ),
                    ),
            ).addClaims(request).asClue {
                it shouldContainAll mapOf("sub" to clientId, "scope" to "testscope:something another:scope", "mock_token_type" to "custom")
            }
        }

        @Test
        fun `token request with aud set as a single string should return it wrapped in a list`() {
            val callback =
                RequestMappingTokenCallback(
                    issuerId = "issuer1",
                    requestMappings =
                        listOf(
                            RequestMapping(
                                requestParam = "grant_type",
                                match = "authorization_code",
                                claims = mapOf("aud" to "my-api"),
                            ),
                        ),
                )
            callback.audience(authCodeRequest()) shouldBe listOf("my-api")
        }

        @Test
        fun `repeated form param values are matched individually, not as a joined string`() {
            val callback =
                RequestMappingTokenCallback(
                    issuerId = "issuer1",
                    requestMappings =
                        listOf(
                            RequestMapping(
                                requestParam = "resource",
                                match = "api://app-b",
                                claims = mapOf("sub" to "matchedByResourceB"),
                            ),
                        ),
                )
            val requestWithRepeatedResource =
                nimbusTokenRequest(
                    clientId,
                    "grant_type" to "client_credentials",
                    "resource" to "api://app-a",
                    "resource" to "api://app-b",
                )
            // Verify both values are preserved in the underlying HTTP request body —
            // nimbusTokenRequest joins pairs via joinToString("&"), so duplicates are not collapsed.
            val bodyParams = requestWithRepeatedResource.toHTTPRequest().bodyAsFormParameters
            bodyParams["resource"] shouldBe listOf("api://app-a", "api://app-b")

            callback.subject(requestWithRepeatedResource) shouldBe "matchedByResourceB"
        }

        @Test
        fun `token request with aud set as a list should return it as-is`() {
            val callback =
                RequestMappingTokenCallback(
                    issuerId = "issuer1",
                    requestMappings =
                        listOf(
                            RequestMapping(
                                requestParam = "grant_type",
                                match = "authorization_code",
                                claims = mapOf("aud" to listOf("my-api", "other-api")),
                            ),
                        ),
                )
            callback.audience(authCodeRequest()) shouldBe listOf("my-api", "other-api")
        }
    }

    @Test
    fun `token request with custom parameters in token request should include claims with placeholder names`() {
        val request =
            clientCredentialsRequest(
                "mock_token_type" to "custom",
                "participantId" to "participantId",
                "actAs" to "actAs",
                "readAs" to "readAs",
            )
        RequestMappingTokenCallback(
            issuerId = "issuer1",
            requestMappings =
                listOf(
                    RequestMapping(
                        requestParam = "mock_token_type",
                        match = "custom",
                        claims =
                            mapOf(
                                "https://daml.com/ledger-api" to
                                    mapOf(
                                        "participantId" to "\${participantId}",
                                        "actAs" to listOf("\${actAs}"),
                                        "readAs" to listOf("\${readAs}"),
                                    ),
                            ),
                    ),
                ),
        ).addClaims(request).asClue {
            it shouldContainAll
                mapOf(
                    "https://daml.com/ledger-api" to
                        mapOf(
                            "participantId" to "participantId",
                            "actAs" to listOf("actAs"),
                            "readAs" to listOf("readAs"),
                        ),
                )
        }
    }

    @Nested
    inner class WithExtraMatchParams {
        private val callback =
            RequestMappingTokenCallback(
                issuerId = "issuer1",
                requestMappings =
                    listOf(
                        RequestMapping(
                            requestParam = "subject",
                            match = "alice",
                            claims = mapOf("role" to "admin", "sub" to "alice"),
                        ),
                        RequestMapping(
                            requestParam = "subject",
                            match = "bob",
                            claims = mapOf("role" to "user", "sub" to "bob"),
                        ),
                        RequestMapping(
                            requestParam = "subject",
                            match = "*",
                            claims = mapOf("role" to "guest", "sub" to "unknown"),
                        ),
                    ),
            )

        @Test
        fun `withExtraMatchParams matches via regex when value comes from extraMatchParams`() {
            val callback =
                RequestMappingTokenCallback(
                    issuerId = "issuer1",
                    requestMappings =
                        listOf(
                            RequestMapping(
                                requestParam = "subject",
                                match = "admin-.*",
                                claims = mapOf("role" to "admin"),
                            ),
                        ),
                )
            val tokenRequest = authCodeRequest()
            callback.withExtraMatchParams(mapOf("subject" to "admin-alice")).addClaims(tokenRequest) shouldContainAll mapOf("role" to "admin")
            callback.withExtraMatchParams(mapOf("subject" to "user-alice")).addClaims(tokenRequest) shouldBe emptyMap()
        }

        @Test
        fun `withExtraMatchParams selects correct requestMapping based on subject`() {
            val tokenRequest = authCodeRequest()
            val aliceCallback = callback.withExtraMatchParams(mapOf("subject" to "alice"))
            val bobCallback = callback.withExtraMatchParams(mapOf("subject" to "bob"))
            assertSoftly {
                aliceCallback.addClaims(tokenRequest) shouldContainAll mapOf("role" to "admin")
                aliceCallback.subject(tokenRequest) shouldBe "alice"
                bobCallback.addClaims(tokenRequest) shouldContainAll mapOf("role" to "user")
                bobCallback.subject(tokenRequest) shouldBe "bob"
            }
        }

        @Test
        fun `withExtraMatchParams falls back to wildcard when subject does not match any specific mapping`() {
            val tokenRequest = authCodeRequest()
            val unknownCallback = callback.withExtraMatchParams(mapOf("subject" to "charlie"))
            assertSoftly {
                unknownCallback.addClaims(tokenRequest) shouldContainAll mapOf("role" to "guest")
            }
        }

        @Test
        fun `form param takes precedence over extraMatchParams on same key`() {
            val callbackWithGrantType =
                RequestMappingTokenCallback(
                    issuerId = "issuer1",
                    requestMappings =
                        listOf(
                            RequestMapping(
                                requestParam = "grant_type",
                                match = "authorization_code",
                                claims = mapOf("role" to "fromForm"),
                            ),
                        ),
                )
            val tokenRequest = authCodeRequest()
            val wrapped = callbackWithGrantType.withExtraMatchParams(mapOf("grant_type" to "something_else"))
            wrapped.addClaims(tokenRequest) shouldContainAll mapOf("role" to "fromForm")
        }

        @Test
        fun `extraMatchParams are available as template variables in claim values`() {
            val callback =
                RequestMappingTokenCallback(
                    issuerId = "issuer1",
                    requestMappings =
                        listOf(
                            RequestMapping(
                                requestParam = "subject",
                                match = "alice",
                                claims = mapOf("preferred_username" to "\${subject}", "role" to "admin"),
                            ),
                        ),
                )
            val tokenRequest = authCodeRequest()
            val wrapped = callback.withExtraMatchParams(mapOf("subject" to "alice"))
            wrapped.addClaims(tokenRequest) shouldContainAll mapOf("preferred_username" to "alice", "role" to "admin")
        }

        @Test
        fun `form params take precedence over extraMatchParams as template variables`() {
            val callback =
                RequestMappingTokenCallback(
                    issuerId = "issuer1",
                    requestMappings =
                        listOf(
                            RequestMapping(
                                requestParam = "subject",
                                match = "*",
                                claims = mapOf("resolved" to "\${grant_type}"),
                            ),
                        ),
                )
            val tokenRequest = authCodeRequest()
            val wrapped = callback.withExtraMatchParams(mapOf("subject" to "alice", "grant_type" to "should_be_overridden"))
            wrapped.addClaims(tokenRequest) shouldContainAll mapOf("resolved" to "authorization_code")
        }

        @Test
        fun `subject is null when mapping does not set sub`() {
            val callback =
                RequestMappingTokenCallback(
                    issuerId = "issuer1",
                    requestMappings =
                        listOf(
                            RequestMapping(
                                requestParam = "subject",
                                match = "alice",
                                claims = mapOf("role" to "admin"),
                            ),
                        ),
                )
            val wrapped = callback.withExtraMatchParams(mapOf("subject" to "alice"))
            wrapped.subject(authCodeRequest()) shouldBe null
        }

        @Test
        fun `subject comes from mapping when sub is set`() {
            val callback =
                RequestMappingTokenCallback(
                    issuerId = "issuer1",
                    requestMappings =
                        listOf(
                            RequestMapping(
                                requestParam = "subject",
                                match = "alice",
                                claims = mapOf("sub" to "mapped-alice", "role" to "admin"),
                            ),
                        ),
                )
            val wrapped = callback.withExtraMatchParams(mapOf("subject" to "alice"))
            wrapped.subject(authCodeRequest()) shouldBe "mapped-alice"
        }

        @Test
        fun `withExtraMatchParams selects typeHeader from matched mapping`() {
            val callback =
                RequestMappingTokenCallback(
                    issuerId = "issuer1",
                    requestMappings =
                        listOf(
                            RequestMapping(
                                requestParam = "subject",
                                match = "alice",
                                claims = mapOf("sub" to "alice"),
                                typeHeader = "at+JWT",
                            ),
                        ),
                )
            val tokenRequest = authCodeRequest()
            val wrapped = callback.withExtraMatchParams(mapOf("subject" to "alice"))
            wrapped.typeHeader(tokenRequest) shouldBe "at+JWT"
        }

        @Test
        fun `withExtraMatchParams selects audience from matched mapping`() {
            val callback =
                RequestMappingTokenCallback(
                    issuerId = "issuer1",
                    requestMappings =
                        listOf(
                            RequestMapping(
                                requestParam = "subject",
                                match = "alice",
                                claims = mapOf("sub" to "alice", "aud" to listOf("my-api")),
                            ),
                        ),
                )
            val tokenRequest = authCodeRequest()
            val wrapped = callback.withExtraMatchParams(mapOf("subject" to "alice"))
            wrapped.audience(tokenRequest) shouldBe listOf("my-api")
        }

        @Test
        fun `withExtraMatchParams selects audience from matched mapping when aud is a string`() {
            val callback =
                RequestMappingTokenCallback(
                    issuerId = "issuer1",
                    requestMappings =
                        listOf(
                            RequestMapping(
                                requestParam = "subject",
                                match = "alice",
                                claims = mapOf("sub" to "alice", "aud" to "my-api"),
                            ),
                        ),
                )
            val tokenRequest = authCodeRequest()
            val wrapped = callback.withExtraMatchParams(mapOf("subject" to "alice"))
            wrapped.audience(tokenRequest) shouldBe listOf("my-api")
        }

        @Test
        fun `withExtraMatchParams selects audience from matched mapping when aud is a list`() {
            val callback =
                RequestMappingTokenCallback(
                    issuerId = "issuer1",
                    requestMappings =
                        listOf(
                            RequestMapping(
                                requestParam = "subject",
                                match = "alice",
                                claims = mapOf("sub" to "alice", "aud" to listOf("my-api", "other-api")),
                            ),
                        ),
                )
            val tokenRequest = authCodeRequest()
            val wrapped = callback.withExtraMatchParams(mapOf("subject" to "alice"))
            wrapped.audience(tokenRequest) shouldBe listOf("my-api", "other-api")
        }

        @Test
        fun `withExtraMatchParams returns empty audience when no mapping matches`() {
            val callback =
                RequestMappingTokenCallback(
                    issuerId = "issuer1",
                    requestMappings =
                        listOf(
                            RequestMapping(
                                requestParam = "subject",
                                match = "alice",
                                claims = mapOf("aud" to listOf("my-api")),
                            ),
                        ),
                )
            val wrapped = callback.withExtraMatchParams(mapOf("subject" to "bob"))
            wrapped.audience(authCodeRequest()) shouldBe emptyList()
        }

        @Test
        fun `withExtraMatchParams returns no match when no mapping matches and no wildcard`() {
            val strictCallback =
                RequestMappingTokenCallback(
                    issuerId = "issuer1",
                    requestMappings =
                        listOf(
                            RequestMapping(
                                requestParam = "subject",
                                match = "alice",
                                claims = mapOf("role" to "admin"),
                            ),
                        ),
                )
            val tokenRequest = authCodeRequest()
            val wrapped = strictCallback.withExtraMatchParams(mapOf("subject" to "bob"))
            wrapped.addClaims(tokenRequest) shouldBe emptyMap()
        }
    }

    @Nested
    inner class DefaultOAuth2TokenCallbacks {
        @Test
        fun `client credentials request should return client_id as sub from callback`() {
            val tokenRequest = clientCredentialsRequest()
            DefaultOAuth2TokenCallback().asClue {
                it.subject(tokenRequest) shouldBe clientId
            }
        }

        @Test
        fun `oidc auth code token request should return scopes not in OIDC from audience in callback`() {
            authCodeRequest().let { tokenRequest ->
                DefaultOAuth2TokenCallback().asClue {
                    it.audience(tokenRequest) shouldBe listOf("default")
                }
            }
            authCodeRequest().let { tokenRequest ->
                DefaultOAuth2TokenCallback().asClue {
                    it.audience(tokenRequest) shouldBe listOf("default")
                }
            }
        }

        @Test
        fun `Allow overriding tid`() {
            val tokenRequest = clientCredentialsRequest()
            DefaultOAuth2TokenCallback().asClue {
                it.addClaims(tokenRequest) shouldContainAll mapOf("tid" to "default")
            }

            DefaultOAuth2TokenCallback(claims = mapOf("tid" to "test-tid")).asClue {
                it.addClaims(tokenRequest) shouldContainAll mapOf("tid" to "test-tid")
            }
        }
    }

    private fun authCodeRequest(vararg formParams: Pair<String, String>) =
        nimbusTokenRequest(
            clientId,
            "grant_type" to "authorization_code",
            "code" to "123",
            *formParams,
        )

    private fun clientCredentialsRequest(vararg formParams: Pair<String, String>) =
        nimbusTokenRequest(clientId, "grant_type" to "client_credentials", *formParams)
}
