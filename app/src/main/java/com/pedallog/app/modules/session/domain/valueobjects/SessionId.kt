package com.pedallog.app.modules.session.domain.valueobjects

/**
 * Representa o identificador único de uma sessão de pedal sincronizada.
 * 
 * Object Calisthenics: Envolva todos os tipos primitivos.
 */
@JvmInline
value class SessionId(val value: String) {
    init {
        require(value.isNotBlank()) { "O identificador da sessão não pode estar vazio" }
    }

    override fun toString(): String = value
}
