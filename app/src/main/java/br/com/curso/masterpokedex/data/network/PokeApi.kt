package br.com.curso.masterpokedex.data.network

import com.google.gson.annotations.SerializedName
import retrofit2.http.GET
import retrofit2.http.Path

// 1. MODELOS DE DADOS (Dicionário da API)
data class PokemonListResponse(
    val results: List<PokemonBrief>
)

data class PokemonBrief(
    val name: String,
    val url: String
)

data class PokemonDetailResponse(
    val id: Int,
    val name: String,
    @SerializedName("sprites") val sprites: SpriteResponse
)

data class SpriteResponse(
    @SerializedName("front_default") val frontDefault: String
)

// 2. O GARÇOM (Retrofit Interface)
interface PokeApiService {
    @GET("pokemon?limit=20")
    suspend fun getList(): PokemonListResponse

    @GET("pokemon/{name}")
    suspend fun getDetail(@Path("name") name: String): PokemonDetailResponse
}