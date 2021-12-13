package hello

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.body
import org.springframework.web.reactive.function.server.router
import reactor.core.publisher.Mono

@SpringBootApplication
class KotlinApplication(val writeCommittedStream: WriteCommittedStream) {


    @Bean
    fun routes() = router {
        GET {
            ServerResponse.ok().body(Mono.just("Let the battle begin!"))
        }

        POST("/**", accept(APPLICATION_JSON)) { request ->
            request.bodyToMono(ArenaUpdate::class.java).flatMap { arenaUpdate ->
                val myState = arenaUpdate.arena.state[arenaUpdate._links.self.href]
                writeCommittedStream.send(arenaUpdate.arena)
                println(arenaUpdate)

                var needRotate = false
                var enemyInRange = false

                arenaUpdate.arena.state.values.forEach { playerState ->
                    when (myState!!.direction) {
                        "N" -> {
                            if (playerState.y < myState.y && playerState.y >= myState.y - 3 && playerState.x == myState.x) {
                                enemyInRange = true
                            }
                        }
                        "S" -> {
                            if (playerState.y > myState.y && playerState.y <= myState.y + 3 && playerState.x == myState.x) {
                                enemyInRange = true
                            }
                        }
                        "W" -> {
                            if (playerState.x < myState.x && playerState.x >= myState.x - 3 && playerState.y == myState.y) {
                                enemyInRange = true
                            }
                        }
                        "E" -> {
                            if (playerState.x > myState.x && playerState.x <= myState.x + 3 && playerState.y == myState.y) {
                                enemyInRange = true
                            }
                        }
                    }
                }
                if (myState!!.y - 3 < 0) {
                    needRotate = true
                } else if (myState.y + 3 > arenaUpdate.arena.dims[1] - 1) {
                    needRotate = true
                } else if (myState.x - 3 < 0) {
                    needRotate = true
                } else if (myState.x + 3 > arenaUpdate.arena.dims[0] - 1) {
                    needRotate = true
                }

                if (needRotate) {
                    ServerResponse.ok().body(Mono.just("F"))
                } else if (myState.wasHit) {
                    ServerResponse.ok().body(Mono.just("F"))
                } else if (enemyInRange) {
                    ServerResponse.ok().body(Mono.just("T"))
                }else {
                    ServerResponse.ok().body(Mono.just(listOf("R", "F").random()))
                }
            }
        }
    }
}

fun main(args: Array<String>) {
    runApplication<KotlinApplication>(*args)
}


data class ArenaUpdate(val _links: Links, val arena: Arena)
data class PlayerState(val x: Int, val y: Int, val direction: String, val score: Int, val wasHit: Boolean)
data class Links(val self: Self)
data class Self(val href: String)
data class Arena(val dims: List<Int>, val state: Map<String, PlayerState>)
