package com.pedallog.app.domain.usecase

import com.pedallog.app.domain.model.PedalPoint
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject

class GetGeoJsonPathUseCase {
    fun invoke(points: List<PedalPoint>): String {
        val coordinates = JsonArray()
        for (point in points) {
            val coord = JsonArray()
            coord.add(point.longitude)
            coord.add(point.latitude)
            coordinates.add(coord)
        }

        val geometry = JsonObject()
        geometry.addProperty("type", "LineString")
        geometry.add("coordinates", coordinates)

        val feature = JsonObject()
        feature.addProperty("type", "Feature")
        feature.add("geometry", geometry)
        feature.add("properties", JsonObject())

        val featureCollection = JsonObject()
        featureCollection.addProperty("type", "FeatureCollection")
        val features = JsonArray()
        features.add(feature)
        featureCollection.add("features", features)

        return Gson().toJson(featureCollection)
    }
}
