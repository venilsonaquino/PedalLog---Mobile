package com.pedallog.app.modules.analysis.application.use_cases

import com.pedallog.app.modules.tracking.domain.entities.PedalPoint
import org.json.JSONArray
import org.json.JSONObject

/**
 * Caso de uso para converter uma lista de pontos em uma String GeoJSON.
 */
class GetGeoJsonPathUseCase {
    operator fun invoke(points: List<PedalPoint>): String {
        if (points.isEmpty()) return ""

        val coordinates = JSONArray()
        points.forEach { point ->
            val coord = JSONArray()
            coord.put(point.coordinate.longitude)
            coord.put(point.coordinate.latitude)
            coordinates.put(coord)
        }

        val geometry = JSONObject()
        geometry.put("type", "LineString")
        geometry.put("coordinates", coordinates)

        val feature = JSONObject()
        feature.put("type", "Feature")
        feature.put("geometry", geometry)
        feature.put("properties", JSONObject())

        val featureCollection = JSONObject()
        featureCollection.put("type", "FeatureCollection")
        val features = JSONArray()
        features.put(feature)
        featureCollection.put("features", features)

        return featureCollection.toString()
    }
}
