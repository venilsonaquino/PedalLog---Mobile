package com.pedallog.app.domain.model

/**
 * Representa o identificador único de uma sessão de pedal sincronizada.
 * 
 * Object Calisthenics: Envolva todos os tipos primitivos.
 * Evita confusão entre diferentes tipos de strings e garante segurança de tipo.
 */
@JvmInline
value class SessionId(val value: String) {
    init {
        require(value.isNotBlank()) { "O identificador da sessão não pode estar vazio" }
    }

    override fun toString(): String = value
}
