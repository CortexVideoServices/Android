package solutions.cvs.videoroom.api

import com.google.gson.annotations.SerializedName
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path


/**
 * Conference data
 */
data class ConferenceData(
    val userData: UserData,
    val description: String,
    @SerializedName("session_id")
    val sessionId: String,
    @SerializedName("display_name")
    val displayName: String,
    @SerializedName("allow_anonymous")
    val allowAnonymous: Boolean
)


/**
 * Create conference request
 */
data class CreateConferenceRequest(
    val description: String,
    @SerializedName("display_name")
    val displayName: String,
    @SerializedName("allow_anonymous")
    val allowAnonymous: Boolean
)


/**
 * Conference REST API interface
 */
interface ConferenceAPI {
    /**
     * Gets conference by session id
     */
    @GET("conference/{session_id}")
    fun getConference(@Path("session_id") sessionId: String): Call<ConferenceData>

    /**
     * Gets current user conference
     */
    @GET("conference")
    fun getCurrentConference(): Call<ConferenceData>

    /**
     * Creates conference
     */
    @POST("conference")
    fun createConference(@Body request: CreateConferenceRequest): Call<Any>
}