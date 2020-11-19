package solutions.cvs.videoroom.api

import com.google.gson.annotations.SerializedName
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST


/**
 * User data
 */
data class UserData(
    @SerializedName("email")
    val email: String,
    @SerializedName("display_name")
    val displayName: String
)


/**
 * Session data
 */
data class SessionData(
    @SerializedName("user_data")
    val userData: UserData,
    @SerializedName("refresh_token")
    val refreshToken: String
)


/**
 * Login request data
 */
data class LoginRequest(
    val username: String,
    val password: String
)


/**
 * Refresh request data
 */
data class RefreshRequest(
    @SerializedName("refresh_token")
    val refreshToken: String
)


/**
 * Users REST API interface
 */
interface UserSessionAPI {
    /**
     * Login request
     */
    @POST("auth/login")
    fun login(@Body request: LoginRequest): Call<SessionData>

    /**
     * Refresh request
     */
    @POST("auth/refresh")
    fun refresh(@Body request: RefreshRequest): Call<SessionData>

    /**
     * Logoff request
     */
    @GET("auth/logoff")
    fun logoff(): Call<ResponseBody>
}