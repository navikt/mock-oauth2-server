package no.nav.security.mock.oauth2.token

import io.kotest.assertions.asClue
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.maps.shouldContainAll
import io.kotest.matchers.maps.shouldNotContainKey
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
    }

    /**
     * Tests for the extraParams feature: auth-request params (e.g. login_hint, acr_values)
     * that are NOT present in the token request body but are propagated via
     * RequestMappingTokenCallback.copy(extraParams = ...) from AuthorizationCodeHandler.
     */
    @Nested
    inner class AuthRequestParamPropagation {

        private val callbackWithLoginHintMappings =
            RequestMappingTokenCallback(
                issuerId = "test-issuer",
                requestMappings =
                    listOf(
                        RequestMapping(
                            requestParam = "login_hint",
                            match = "anna@example.com",
                            claims =
                                mapOf(
                                    "sub" to "anna-uuid",
                                    "email" to "anna@example.com",
                                    "urn:telematik:claims:id" to "X111111111",
                                ),
                        ),
                        RequestMapping(
                            requestParam = "login_hint",
                            match = "max@example.com",
                            claims =
                                mapOf(
                                    "sub" to "max-uuid",
                                    "email" to "max@example.com",
                                    "urn:telematik:claims:id" to "X222222222",
                                ),
                        ),
                        // Fallback: match any auth-code grant when no login_hint mapping fires
                        RequestMapping(
                            requestParam = "grant_type",
                            match = "authorization_code",
                            claims =
                                mapOf(
                                    "sub" to "default-uuid",
                                    "email" to "default@example.com",
                                ),
                        ),
                    ),
            )

        @Test
        fun `login_hint in extraParams selects the correct mapping and returns matching claims`() {
            val tokenRequest = authCodeRequest()
            val enriched = callbackWithLoginHintMappings.copy(extraParams = mapOf("login_hint" to "anna@example.com"))

            enriched.addClaims(tokenRequest).asClue {
                it shouldContainAll
                    mapOf(
                        "sub" to "anna-uuid",
                        "email" to "anna@example.com",
                        "urn:telematik:claims:id" to "X111111111",
                    )
            }
            enriched.subject(tokenRequest) shouldBe "anna-uuid"
        }

        @Test
        fun `different login_hint in extraParams selects a different mapping`() {
            val tokenRequest = authCodeRequest()
            val enriched = callbackWithLoginHintMappings.copy(extraParams = mapOf("login_hint" to "max@example.com"))

            enriched.addClaims(tokenRequest).asClue {
                it shouldContainAll
                    mapOf(
                        "sub" to "max-uuid",
                        "email" to "max@example.com",
                        "urn:telematik:claims:id" to "X222222222",
                    )
            }
            enriched.subject(tokenRequest) shouldBe "max-uuid"
        }

        @Test
        fun `absent login_hint falls through to next matching mapping (grant_type wildcard)`() {
            val tokenRequest = authCodeRequest()
            // no extraParams -> login_hint mappings won't match -> falls through to grant_type mapping
            val enriched = callbackWithLoginHintMappings.copy(extraParams = emptyMap())

            enriched.addClaims(tokenRequest).asClue {
                it shouldContainAll mapOf("sub" to "default-uuid", "email" to "default@example.com")
                it shouldNotContainKey "urn:telematik:claims:id"
            }
        }

        @Test
        fun `login_hint value is available as dollar-brace template in claim values`() {
            val tokenRequest = authCodeRequest()
            val callbackWithTemplate =
                RequestMappingTokenCallback(
                    issuerId = "test-issuer",
                    requestMappings =
                        listOf(
                            RequestMapping(
                                requestParam = "grant_type",
                                match = "authorization_code",
                                claims =
                                    mapOf(
                                        // The hint value should be substituted at token-generation time
                                        "email" to "\${login_hint}",
                                        "sub" to "fixed-sub",
                                    ),
                            ),
                        ),
                    extraParams = mapOf("login_hint" to "substituted@example.com"),
                )

            callbackWithTemplate.addClaims(tokenRequest).asClue {
                it shouldContainAll
                    mapOf(
                        "email" to "substituted@example.com",
                        "sub" to "fixed-sub",
                    )
            }
        }

        @Test
        fun `multiple extraParams are all available for matching and template substitution`() {
            val tokenRequest = authCodeRequest()
            val callback =
                RequestMappingTokenCallback(
                    issuerId = "test-issuer",
                    requestMappings =
                        listOf(
                            RequestMapping(
                                requestParam = "acr_values",
                                match = "gematik-ehealth-loa-high",
                                claims =
                                    mapOf(
                                        "acr" to "gematik-ehealth-loa-high",
                                        "email" to "\${login_hint}",
                                    ),
                            ),
                        ),
                    extraParams =
                        mapOf(
                            "acr_values" to "gematik-ehealth-loa-high",
                            "login_hint" to "multi@example.com",
                        ),
                )

            callback.addClaims(tokenRequest).asClue {
                it shouldContainAll
                    mapOf(
                        "acr" to "gematik-ehealth-loa-high",
                        "email" to "multi@example.com",
                    )
            }
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
