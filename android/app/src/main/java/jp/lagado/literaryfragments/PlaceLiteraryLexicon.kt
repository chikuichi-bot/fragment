package jp.lagado.literaryfragments

/**
 * ISO country → literary English place terms for atmosphere search.
 * Place keywords lead; climate/time trail (matching iOS PlaceLiteraryLexicon).
 */
object PlaceLiteraryLexicon {
    private val byISO: Map<String, List<String>> = mapOf(
        "JP" to listOf("Japan", "Japanese", "Tokyo", "Kyoto", "Osaka", "Edo", "Nippon", "Fuji"),
        "IT" to listOf("Italy", "Italian", "Rome", "Venice", "Florence", "Naples", "Sicily", "Roman"),
        "EG" to listOf("Egypt", "Egyptian", "Nile", "Cairo", "Alexandria", "Pyramid", "Pharaoh"),
        "US" to listOf("America", "American", "New York", "Boston", "Chicago", "Mississippi"),
        "GB" to listOf("England", "English", "London", "Britain", "British", "Thames"),
        "UK" to listOf("England", "English", "London", "Britain", "British", "Thames"),
        "FR" to listOf("France", "French", "Paris", "Seine", "Provence"),
        "DE" to listOf("Germany", "German", "Berlin", "Rhine", "Bavaria"),
        "CN" to listOf("China", "Chinese", "Peking", "Beijing", "Shanghai", "Yangtze"),
        "KR" to listOf("Korea", "Korean", "Seoul"),
        "ES" to listOf("Spain", "Spanish", "Madrid", "Barcelona", "Andalusia"),
        "PT" to listOf("Portugal", "Portuguese", "Lisbon"),
        "RU" to listOf("Russia", "Russian", "Moscow", "Petersburg", "Siberia"),
        "IN" to listOf("India", "Indian", "Delhi", "Bombay", "Ganges", "Calcutta"),
        "GR" to listOf("Greece", "Greek", "Athens", "Sparta", "Aegean"),
        "TR" to listOf("Turkey", "Turkish", "Istanbul", "Constantinople", "Ottoman"),
        "BR" to listOf("Brazil", "Brazilian", "Rio", "Amazon"),
        "MX" to listOf("Mexico", "Mexican", "Aztec"),
        "CA" to listOf("Canada", "Canadian", "Montreal", "Quebec"),
        "AU" to listOf("Australia", "Australian", "Sydney", "Melbourne"),
        "NZ" to listOf("Zealand", "Maori"),
        "IE" to listOf("Ireland", "Irish", "Dublin"),
        "NL" to listOf("Holland", "Dutch", "Amsterdam"),
        "SE" to listOf("Sweden", "Swedish", "Stockholm"),
        "NO" to listOf("Norway", "Norwegian", "fjord"),
        "DK" to listOf("Denmark", "Danish", "Copenhagen"),
        "FI" to listOf("Finland", "Finnish"),
        "PL" to listOf("Poland", "Polish", "Warsaw"),
        "CZ" to listOf("Bohemia", "Prague", "Czech"),
        "AT" to listOf("Austria", "Austrian", "Vienna"),
        "CH" to listOf("Switzerland", "Swiss", "Alpine"),
        "BE" to listOf("Belgium", "Belgian", "Brussels"),
        "AR" to listOf("Argentina", "Argentine", "Buenos Aires"),
        "ZA" to listOf("Africa", "African", "Cape"),
        "NG" to listOf("Africa", "African", "Nigeria"),
        "KE" to listOf("Africa", "African", "Kenya"),
        "MA" to listOf("Morocco", "Moorish", "Casablanca"),
        "SA" to listOf("Arabia", "Arabian", "Mecca"),
        "AE" to listOf("Arabia", "Arabian", "desert"),
        "IL" to listOf("Jerusalem", "Israel", "Palestine", "Hebrew"),
        "IR" to listOf("Persia", "Persian", "Iran"),
        "IQ" to listOf("Babylon", "Baghdad", "Mesopotamia"),
        "TH" to listOf("Siam", "Thailand", "Bangkok"),
        "VN" to listOf("Vietnam", "Annam", "Saigon"),
        "PH" to listOf("Philippine", "Manila"),
        "ID" to listOf("Java", "Bali", "Indies"),
        "SG" to listOf("Singapore"),
        "TW" to listOf("Formosa", "Taiwan"),
        "HK" to listOf("Hong Kong", "China"),
        "PE" to listOf("Peru", "Inca", "Andes"),
        "CL" to listOf("Chile", "Andes"),
        "CO" to listOf("Colombia", "Andes"),
        "CU" to listOf("Cuba", "Cuban", "Havana"),
        "IS" to listOf("Iceland", "Icelandic"),
        "UA" to listOf("Ukraine", "Ukrainian", "Kiev"),
        "HU" to listOf("Hungary", "Hungarian", "Budapest"),
        "RO" to listOf("Romania", "Romanian", "Danube")
    )

    fun uniquePreserve(items: List<String>): List<String> {
        val seen = LinkedHashSet<String>()
        val out = mutableListOf<String>()
        for (raw in items) {
            val t = raw.trim()
            if (t.isEmpty()) continue
            val key = t.lowercase()
            if (seen.add(key)) out.add(t)
        }
        return out
    }

    data class PlaceBuild(
        val search: List<String>,
        val displayPlace: String,
        val placeKeysForRank: List<String>
    )

    fun build(
        isoCountryCode: String?,
        countryName: String?,
        adminArea: String?,
        city: String?,
        preferJapaneseDisplay: Boolean
    ): PlaceBuild {
        val search = mutableListOf<String>()
        val iso = (isoCountryCode ?: "").uppercase()
        byISO[iso]?.let { lex ->
            search.addAll(lex.take(6))
            lex.firstOrNull()?.let { head ->
                search.add(0, head)
                search.add(0, head)
            }
        }
        if (!countryName.isNullOrBlank()) search.add(countryName)
        if (!adminArea.isNullOrBlank()) search.add(adminArea)
        if (!city.isNullOrBlank()) search.add(city)
        val unique = uniquePreserve(search)

        val displayCountry = when {
            preferJapaneseDisplay && !countryName.isNullOrBlank() -> countryName
            byISO[iso]?.firstOrNull() != null -> byISO[iso]!!.first()
            !countryName.isNullOrBlank() -> countryName
            else -> iso
        }
        val displayCity = city.orEmpty()
        val displayPlace = when {
            preferJapaneseDisplay -> when {
                displayCountry.isNotEmpty() && displayCity.isNotEmpty() -> "$displayCountry・$displayCity"
                displayCountry.isNotEmpty() -> displayCountry
                else -> displayCity
            }
            else -> when {
                displayCountry.isNotEmpty() && displayCity.isNotEmpty() -> "$displayCountry · $displayCity"
                displayCountry.isNotEmpty() -> displayCountry
                else -> displayCity
            }
        }
        return PlaceBuild(
            search = unique,
            displayPlace = displayPlace,
            placeKeysForRank = unique.take(8)
        )
    }
}
