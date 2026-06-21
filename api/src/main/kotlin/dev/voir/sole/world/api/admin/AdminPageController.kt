package dev.voir.sole.world.api.admin

import org.springframework.stereotype.Controller
import org.springframework.security.web.csrf.CsrfToken
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ResponseBody

/** Serves the small static admin UI from stable, extensionless routes. */
@Controller
class AdminPageController {
    @GetMapping("/admin")
    fun admin(): String = "forward:/admin/index.html"

    @GetMapping("/admin/login")
    fun login(): String = "forward:/admin/login.html"

    @GetMapping("/admin/api/csrf")
    @ResponseBody
    fun csrf(csrfToken: CsrfToken): CsrfTokenResponse {
        return CsrfTokenResponse(
            headerName = csrfToken.headerName,
            parameterName = csrfToken.parameterName,
            token = csrfToken.token,
        )
    }
}

data class CsrfTokenResponse(
    val headerName: String,
    val parameterName: String,
    val token: String,
)
