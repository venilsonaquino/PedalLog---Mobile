package com.pedallog.app.shared.infraestructure

/**
 * Interface base para mapeamento entre Entidades de Domínio e Models de Infraestrutura.
 * 
 * DDD: Mantém a separação entre o Domínio e a Infraestrutura (DB).
 */
interface Mapper<Entity, Model> {
    fun toEntity(model: Model): Entity
    fun toModel(entity: Entity): Model
}
