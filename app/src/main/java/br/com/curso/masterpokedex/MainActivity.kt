package br.com.curso.masterpokedex

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import br.com.curso.masterpokedex.data.network.PokeApiService
import br.com.curso.masterpokedex.data.network.PokemonBrief
import coil.compose.AsyncImage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MasterPokedexTheme {   // aplica as cores da Pokédex em todo o app
                PokedexApp()
            }
        }
    }
}

/**
 * Tema customizado com paleta Pokédex (vermelho + verde neon)
 */
@Composable
fun MasterPokedexTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary   = Color(0xFFFF1744),  // vermelho Pokédex
            secondary = Color(0xFF00E676),  // verde neon
            background = Color(0xFF121212),
            surface    = Color(0xFF1E1E1E)
        ),
        content = content
    )
}

/**
 * Tema customizado com paleta Pokédex (vermelho + verde neon)
 */

class PokedexViewModel : ViewModel() {

    // Instância do Retrofit com baseUrl e conversor GSON
    private val retrofit = Retrofit.Builder()
        .baseUrl("https://pokeapi.co/api/v2/")   // endereço base da PokeAPI
        .addConverterFactory(GsonConverterFactory.create())  // plugin de tradução JSON→Kotlin
        .build()

    // Cria o "garçom" a partir da interface que definimos em PokeApi.kt
    val apiService: PokeApiService = retrofit.create(PokeApiService::class.java)

    // ── Os três estados reativos que a UI observa ──────────────────────────
    var pokemonList by mutableStateOf<List<PokemonBrief>>(emptyList())
    //   ↑ lista preenchida após resposta bem-sucedida

    var isLoading by mutableStateOf(false)
    //   ↑ true enquanto aguarda resposta da rede

    var error by mutableStateOf<String?>(null)
    //   ↑ mensagem de erro caso a chamada falhe; null quando tudo está ok

    // Busca a lista de Pokémons. Extraída em uma função própria (em vez de ficar
    // direto no LaunchedEffect) para poder ser chamada de novo pelo botão "Tentar novamente".
    suspend fun carregarPokemons() {
        isLoading = true
        error = null
        try {
            val response = apiService.getList()
            pokemonList = response.results
        } catch (e: CancellationException) {
            throw e   // ↑ IMPORTANTE: nunca capturar o cancelamento da coroutine como se fosse erro
        } catch (e: Exception) {
            error = "Não foi possível carregar os Pokémons. Verifique sua conexão."
        } finally {
            isLoading = false
        }
    }
}

/**
 * Tela principal da Pokédex com gerenciamento de estados de rede e grade de cartões.
 */
/**
 * Tela principal da Pokédex com gerenciamento de estados de rede e grade de cartões.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PokedexApp(viewModel: PokedexViewModel = viewModel()) {

    // CoroutineScope ligado ao ciclo de vida da composição — usado para o clique
    // do botão "Tentar novamente" disparar uma nova busca fora do LaunchedEffect
    val coroutineScope = rememberCoroutineScope()

    // LaunchedEffect(Unit): executa o bloco UMA ÚNICA VEZ ao entrar na tela
    LaunchedEffect(Unit) {
        viewModel.carregarPokemons()   // toda a lógica de loading/try-catch mora no ViewModel (5.2)
    }

    // Gradiente vermelho escuro → preto como fundo da tela
    val backgroundGradient = Brush.verticalGradient(
        colors = listOf(Color(0xFF8E0E00), Color(0xFF1F1C1C))
    )

    Box(modifier = Modifier.fillMaxSize().background(backgroundGradient)) {
        Scaffold(
            containerColor = Color.Transparent,  // deixa o gradiente aparecer atrás
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            "MASTER POKÉDEX",
                            fontWeight = FontWeight.Black,
                            letterSpacing = 2.sp,
                            color = Color.White
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            }
        ) { padding ->
            Box(modifier = Modifier.padding(padding).fillMaxSize()) {
                when {
                    // Estado 1 — Carregando: exibe o anel giratório centralizado
                    viewModel.isLoading -> CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = Color(0xFFFF1744)
                    )

                    // Estado 2 — Erro de rede: exibe a mensagem e um botão para tentar de novo
                    viewModel.error != null -> Column(
                        modifier = Modifier.align(Alignment.Center).padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Erro: ${viewModel.error}",
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { coroutineScope.launch { viewModel.carregarPokemons() } }) {
                            Text("Tentar novamente")
                        }
                    }

                    // Estado 3 — Sucesso: exibe a grade de cartões
                    else -> LazyVerticalGrid(
                        columns = GridCells.Fixed(2),          // sempre 2 colunas
                        contentPadding = PaddingValues(8.dp)   // espaço nas bordas da grade
                    ) {
                        items(viewModel.pokemonList) { pokemon ->
                            PokemonCard(pokemon)
                        }
                    }
                }
            }
        }
    }
}
/**
 * Cartão individual de Pokémon com imagem oficial e número formatado
 */
/**
 * Cartão individual de Pokémon com imagem oficial e número formatado
 */
@Composable
fun PokemonCard(pokemon: PokemonBrief) {

    // A URL tem o formato: "https://pokeapi.co/api/v2/pokemon/25/"
    // trimEnd('/') remove a barra final → "https://pokeapi.co/api/v2/pokemon/25"
    // substringAfterLast('/') pega o último trecho → "25"
    // toIntOrNull() converte para Int, ou null se o formato vier diferente do esperado
    val id = pokemon.url.trimEnd('/').substringAfterLast('/').toIntOrNull()

    // Arte oficial em PNG hospedada no GitHub do projeto PokeAPI
    // Se id for null, imageUrl também fica null — o AsyncImage cai no estado de erro
    // (bloco abaixo) em vez de montar uma URL quebrada com "null.png"
    val imageUrl = id?.let {
        "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/other/official-artwork/$it.png"
    }

    Card(
        modifier = Modifier
            .padding(8.dp)
            .fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // AsyncImage (Coil): baixa e exibe a imagem da nuvem de forma assíncrona
            // placeholder = enquanto carrega | error = se falhar (ou se model for null)
            // Sem esses dois parâmetros, uma falha de rede deixaria um espaço em branco sem feedback nenhum
            AsyncImage(
                model = imageUrl,
                contentDescription = pokemon.name,
                placeholder = ColorPainter(Color.White.copy(alpha = 0.1f)),
                error = ColorPainter(Color.White.copy(alpha = 0.1f)),
                modifier = Modifier.size(100.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = pokemon.name.uppercase(),        // "bulbasaur" → "BULBASAUR"
                fontWeight = FontWeight.Bold,
                color = Color.White,
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )
            Text(
                // 1 → "#001", 25 → "#025"; se o id não pôde ser extraído, mostra "#???" em vez de mentir "#001"
                text = id?.let { "#${it.toString().padStart(3, '0')}" } ?: "#???",
                color = Color.Gray,
                fontSize = 12.sp
            )
        }
    }
}
