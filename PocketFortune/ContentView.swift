import SwiftUI
import WebKit
import AVFoundation
import UIKit
import Translation
import Combine
import CoreLocation
import StoreKit

extension Notification.Name {
    static let forceDisplayQuote = Notification.Name("forceDisplayQuote")
    static let closeSettings = Notification.Name("closeSettings")
}

class TicketManager: ObservableObject {
    static let shared = TicketManager()
    
    @Published var paidTickets: Int {
        didSet { UserDefaults.standard.set(paidTickets, forKey: "remainingTickets") }
    }
    @Published var freeTickets: Int {
        didSet { UserDefaults.standard.set(freeTickets, forKey: "freeTickets") }
    }
    
    init() {
        if UserDefaults.standard.object(forKey: "remainingTickets") == nil {
            UserDefaults.standard.set(0, forKey: "remainingTickets")
        }
        if UserDefaults.standard.object(forKey: "freeTickets") == nil {
            UserDefaults.standard.set(3, forKey: "freeTickets")
        }
        self.paidTickets = UserDefaults.standard.integer(forKey: "remainingTickets")
        self.freeTickets = UserDefaults.standard.integer(forKey: "freeTickets")
        checkDailyReset()
    }
    
    func checkDailyReset() {
        let formatter = DateFormatter()
        formatter.dateFormat = "yyyy-MM-dd"
        let todayString = formatter.string(from: Date())
        let lastDate = UserDefaults.standard.string(forKey: "lastFreeAITicketDate") ?? ""
        
        if todayString != lastDate {
            freeTickets = 3
            UserDefaults.standard.set(todayString, forKey: "lastFreeAITicketDate")
        }
    }
    
    func consumeTicket() -> Bool {
        checkDailyReset()
        if freeTickets > 0 {
            freeTickets -= 1
            return true
        } else if paidTickets > 0 {
            paidTickets -= 1
            return true
        }
        return false
    }
}

enum IAPProduct: String, CaseIterable {
    case ticket100 = "jp.lagado.literaryfragments.ticket100"
    case ticket1000 = "jp.lagado.literaryfragments.ticket1000"
    case ticket10000 = "jp.lagado.literaryfragments.ticket10000"

    var ticketAmount: Int {
        switch self {
        case .ticket100: return 100
        case .ticket1000: return 1000
        case .ticket10000: return 10000
        }
    }
}

@MainActor
class StoreManager: ObservableObject {
    @Published var products: [Product] = []
    @Published var isPurchasing = false
    @Published var isLoadingProducts = true
    var updateListenerTask: Task<Void, Error>? = nil

    init() {
        updateListenerTask = listenForTransactions()
        Task {
            await fetchProducts()
        }
    }

    deinit {
        updateListenerTask?.cancel()
    }

    func fetchProducts() async {
        self.isLoadingProducts = true
        do {
            let productIDs = IAPProduct.allCases.map { $0.rawValue }
            let fetchedProducts = try await Product.products(for: productIDs)
            self.products = fetchedProducts.sorted { $0.price < $1.price }
            self.isLoadingProducts = false
        } catch {
            self.isLoadingProducts = false
            print("商品の取得に失敗しました: \(error)")
        }
    }

    func purchase(_ product: Product) async throws {
        isPurchasing = true
        defer { isPurchasing = false }
        
        let result = try await product.purchase()
        
        switch result {
        case .success(let verification):
            let transaction = try checkVerified(verification)
            await grantTickets(for: transaction)
            await transaction.finish()
        case .userCancelled, .pending:
            break
        @unknown default:
            break
        }
    }

    func listenForTransactions() -> Task<Void, Error> {
        return Task.detached {
            for await result in StoreKit.Transaction.updates {
                do {
                    let transaction = try await self.checkVerified(result)
                    await self.grantTickets(for: transaction)
                    await transaction.finish()
                } catch {
                    print("取引の検証に失敗しました")
                }
            }
        }
    }

    func checkVerified<T>(_ result: VerificationResult<T>) throws -> T {
        switch result {
        case .unverified:
            throw StoreError.failedVerification
        case .verified(let safe):
            return safe
        }
    }

    func grantTickets(for transaction: StoreKit.Transaction) async {
        guard let productType = IAPProduct(rawValue: transaction.productID) else { return }
        
        DispatchQueue.main.async {
            TicketManager.shared.paidTickets += productType.ticketAmount
            UIImpactFeedbackGenerator(style: .heavy).impactOccurred()
        }
    }
    
    enum StoreError: Error {
        case failedVerification
    }
}

struct UIStrings: Codable {
    var settingsTitle, nativeLanguageTitle, aiLevelTitle, done, ticketStore: String
    var selectLanguage, stockTitle, emptyStock, viewExplanation, close: String
    var aiExplanationTitle, generatingText, outOfTicketsTitle, outOfTicketsMsg: String
    var buyButton, cancel, status, remainingTickets: String
    var level1, level2, level3, level4: String
    var level1Desc, level2Desc, level3Desc, level4Desc: String
    var confirmExplanationTitle, confirmExplanationMsg, generateButton: String
    var onboardingTitle, onboardingDesc, nextButton, startButton: String
    var tutorialFeaturesTitle, feature1Title, feature1Desc, feature2Title, feature2Desc, feature3Title, feature3Desc: String
    var databaseSearch, searchPlaceholder, searchEmpty: String
    var historyTitle, emptyHistory: String
    var quoteLengthTitle, lengthShort, lengthLong: String
    var ticketDesc: String

    static func defaultEnglish() -> UIStrings {
        UIStrings(
            settingsTitle: "Settings",
            nativeLanguageTitle: "Your Native Language",
            aiLevelTitle: "AI Explanation Level",
            done: "Done",
            ticketStore: "Ticket Store",
            selectLanguage: "Select your language",
            stockTitle: "Stocked Quotes",
            emptyStock: "No quotes stocked yet.",
            viewExplanation: "View Explanation",
            close: "Close",
            aiExplanationTitle: "AI Explanation",
            generatingText: "Generating...\nPlease wait⏳",
            outOfTicketsTitle: "Out of Tickets",
            outOfTicketsMsg: "You need tickets for AI Explanation.",
            buyButton: "Go to Store",
            cancel: "Cancel",
            status: "Status",
            remainingTickets: "Tickets",
            level1: "Middle School",
            level2: "High School",
            level3: "College",
            level4: "Business",
            level1Desc: "Avoids difficult grammar terms and explains basic sentence structures gently. Great for beginners.",
            level2Desc: "Points out important grammar for exams and explains logical sentence structures.",
            level3Desc: "Explores literary metaphors, nuances, and cultural backgrounds for advanced learners.",
            level4Desc: "Focuses on formality and how to use expressions in practical professional situations.",
            confirmExplanationTitle: "Confirm",
            confirmExplanationMsg: "Use 1 ticket to generate an AI explanation?",
            generateButton: "Generate",
            onboardingTitle: "Welcome to Fragments",
            onboardingDesc: "Discover approx. 43 million fragments extracted from 60,000 books, displayed at random. Save your favorites and use AI explanations to deepen your understanding.",
            nextButton: "Next",
            startButton: "Start",
            tutorialFeaturesTitle: "How to Use",
            feature1Title: "Translate Words",
            feature1Desc: "Tap any word to use the built-in translation.",
            feature2Title: "Search Web",
            feature2Desc: "Tap the author or title to search the book on the web.",
            feature3Title: "Draw a Quote",
            feature3Desc: "Swipe horizontally to draw a new quote.",
            databaseSearch: "Database Search",
            searchPlaceholder: "Search by keyword...",
            searchEmpty: "No results found.",
            historyTitle: "Display History",
            emptyHistory: "No history yet.",
            quoteLengthTitle: "Quote Length",
            lengthShort: "Short",
            lengthLong: "Long",
            ticketDesc: "A ticket to unlock AI explanations."
        )
    }
}

struct ChatMessage: Identifiable, Codable, Equatable {
    var id = UUID()
    let isUser: Bool
    let text: String
}

struct StockedQuote: Codable, Hashable {
    let text: String
    var title: String?
    var author: String?
    let date: Date
}

enum SearchScope: CaseIterable {
    case all, quote, author, title
    
    func localizedName(isJapanese: Bool) -> String {
        switch self {
        case .all: return isJapanese ? "すべて" : "All"
        case .quote: return isJapanese ? "言葉" : "Quote"
        case .author: return isJapanese ? "著者" : "Author"
        case .title: return isJapanese ? "作品名" : "Title"
        }
    }
}

/// Gutenberg 英語コーパス向けの「国・土地」文学キーワード。気配検索では気候より優先する。
enum PlaceLiteraryLexicon {
    static let byISO: [String: [String]] = [
        "JP": ["Japan", "Japanese", "Tokyo", "Kyoto", "Osaka", "Edo", "Nippon", "Fuji"],
        "IT": ["Italy", "Italian", "Rome", "Venice", "Florence", "Naples", "Sicily", "Roman"],
        "EG": ["Egypt", "Egyptian", "Nile", "Cairo", "Alexandria", "Pyramid", "Pharaoh"],
        "US": ["America", "American", "New York", "Boston", "Chicago", "Mississippi"],
        "GB": ["England", "English", "London", "Britain", "British", "Thames"],
        "UK": ["England", "English", "London", "Britain", "British", "Thames"],
        "FR": ["France", "French", "Paris", "Seine", "Provence"],
        "DE": ["Germany", "German", "Berlin", "Rhine", "Bavaria"],
        "CN": ["China", "Chinese", "Peking", "Beijing", "Shanghai", "Yangtze"],
        "KR": ["Korea", "Korean", "Seoul"],
        "ES": ["Spain", "Spanish", "Madrid", "Barcelona", "Andalusia"],
        "PT": ["Portugal", "Portuguese", "Lisbon"],
        "RU": ["Russia", "Russian", "Moscow", "Petersburg", "Siberia"],
        "IN": ["India", "Indian", "Delhi", "Bombay", "Ganges", "Calcutta"],
        "GR": ["Greece", "Greek", "Athens", "Sparta", "Aegean"],
        "TR": ["Turkey", "Turkish", "Istanbul", "Constantinople", "Ottoman"],
        "BR": ["Brazil", "Brazilian", "Rio", "Amazon"],
        "MX": ["Mexico", "Mexican", "Aztec"],
        "CA": ["Canada", "Canadian", "Montreal", "Quebec"],
        "AU": ["Australia", "Australian", "Sydney", "Melbourne"],
        "NZ": ["Zealand", "Maori"],
        "IE": ["Ireland", "Irish", "Dublin"],
        "NL": ["Holland", "Dutch", "Amsterdam"],
        "SE": ["Sweden", "Swedish", "Stockholm"],
        "NO": ["Norway", "Norwegian", "fjord"],
        "DK": ["Denmark", "Danish", "Copenhagen"],
        "FI": ["Finland", "Finnish"],
        "PL": ["Poland", "Polish", "Warsaw"],
        "CZ": ["Bohemia", "Prague", "Czech"],
        "AT": ["Austria", "Austrian", "Vienna", "Vienna"],
        "CH": ["Switzerland", "Swiss", "Alpine"],
        "BE": ["Belgium", "Belgian", "Brussels"],
        "AR": ["Argentina", "Argentine", "Buenos Aires"],
        "ZA": ["Africa", "African", "Cape"],
        "NG": ["Africa", "African", "Nigeria"],
        "KE": ["Africa", "African", "Kenya"],
        "MA": ["Morocco", "Moorish", "Casablanca"],
        "SA": ["Arabia", "Arabian", "Mecca"],
        "AE": ["Arabia", "Arabian", "desert"],
        "IL": ["Jerusalem", "Israel", "Palestine", "Hebrew"],
        "IR": ["Persia", "Persian", "Iran"],
        "IQ": ["Babylon", "Baghdad", "Mesopotamia"],
        "TH": ["Siam", "Thailand", "Bangkok"],
        "VN": ["Vietnam", "Annam", "Saigon"],
        "PH": ["Philippine", "Manila"],
        "ID": ["Java", "Bali", "Indies"],
        "SG": ["Singapore"],
        "TW": ["Formosa", "Taiwan"],
        "HK": ["Hong Kong", "China"],
        "PE": ["Peru", "Inca", "Andes"],
        "CL": ["Chile", "Andes"],
        "CO": ["Colombia", "Andes"],
        "CU": ["Cuba", "Cuban", "Havana"],
        "IS": ["Iceland", "Icelandic"],
        "UA": ["Ukraine", "Ukrainian", "Kiev"],
        "HU": ["Hungary", "Hungarian", "Budapest"],
        "RO": ["Romania", "Romanian", "Danube"]
    ]

    static func uniquePreserve(_ items: [String]) -> [String] {
        var seen = Set<String>()
        var out: [String] = []
        for raw in items {
            let t = raw.trimmingCharacters(in: .whitespacesAndNewlines)
            guard !t.isEmpty else { continue }
            let key = t.lowercased()
            if seen.insert(key).inserted { out.append(t) }
        }
        return out
    }

    /// 検索用（英語中心）と表示用の地名ラベルを返す。国系キーワードを先頭に厚く積む。
    static func build(
        isoCountryCode: String?,
        countryName: String?,
        adminArea: String?,
        city: String?,
        preferJapaneseDisplay: Bool
    ) -> (search: [String], displayPlace: String) {
        var search: [String] = []
        let iso = (isoCountryCode ?? "").uppercased()
        if let lex = byISO[iso] {
            search.append(contentsOf: Array(lex.prefix(6)))
            // 国ヒット率を上げるため先頭語を重ねる
            if let head = lex.first { search.insert(head, at: 0); search.insert(head, at: 0) }
        }
        if let countryName = countryName, !countryName.isEmpty {
            search.append(countryName)
        }
        if let adminArea = adminArea, !adminArea.isEmpty {
            search.append(adminArea)
        }
        if let city = city, !city.isEmpty {
            search.append(city)
        }
        search = uniquePreserve(search)

        let displayCountry: String = {
            if preferJapaneseDisplay {
                // reverseGeocode の preferredLocale が ja なら country が日本語になりやすい
                return countryName?.isEmpty == false ? (countryName ?? "") : (byISO[iso]?.first ?? iso)
            }
            return byISO[iso]?.first ?? countryName ?? iso
        }()
        let displayCity = city ?? ""
        let displayPlace: String = {
            if preferJapaneseDisplay {
                if !displayCountry.isEmpty && !displayCity.isEmpty { return "\(displayCountry)・\(displayCity)" }
                if !displayCountry.isEmpty { return displayCountry }
                return displayCity
            }
            if !displayCountry.isEmpty && !displayCity.isEmpty { return "\(displayCountry) · \(displayCity)" }
            if !displayCountry.isEmpty { return displayCountry }
            return displayCity
        }()
        return (search, displayPlace)
    }
}

class AtmosphereManager: NSObject, ObservableObject, CLLocationManagerDelegate {
    static let shared = AtmosphereManager()
    private let locationManager = CLLocationManager()
    
    @Published var isSensing = false
    @Published var sensingStatus = ""
    
    private var currentLanguage = "English"
    private var completion: (([String], String) -> Void)?
    
    override init() {
        super.init()
        locationManager.delegate = self
    }
    
    func senseMoment(language: String, completion: @escaping ([String], String) -> Void) {
        self.currentLanguage = language
        self.completion = completion
        DispatchQueue.main.async {
            self.isSensing = true
            self.sensingStatus = "Sensing..."
        }
        
        let status = locationManager.authorizationStatus
        if status == .notDetermined {
            locationManager.requestWhenInUseAuthorization()
        } else if status == .denied || status == .restricted {
            failToKyoto()
        } else {
            locationManager.requestLocation()
        }
    }
    
    func locationManagerDidChangeAuthorization(_ manager: CLLocationManager) {
        let status = manager.authorizationStatus
        if status == .authorizedWhenInUse || status == .authorizedAlways {
            if isSensing {
                manager.requestLocation()
            }
        } else if status == .denied || status == .restricted {
            if isSensing {
                failToKyoto()
            }
        }
    }
    
    private func failToKyoto() {
        let isJapanese = currentLanguage == "日本語" || currentLanguage.contains("Japanese")
        fetchWeather(
            lat: 35.0116,
            lon: 135.7681,
            iso: "JP",
            country: isJapanese ? "日本" : "Japan",
            admin: isJapanese ? "京都府" : "Kyoto",
            city: isJapanese ? "京都市" : "Kyoto"
        )
    }
    
    func locationManager(_ manager: CLLocationManager, didUpdateLocations locations: [CLLocation]) {
        guard let loc = locations.first else { failToKyoto(); return }
        
        let isJapanese = currentLanguage == "日本語" || currentLanguage.contains("Japanese")
        let locale = Locale(identifier: isJapanese ? "ja_JP" : "en_US")
        
        Task {
            let geocoder = CLGeocoder()
            do {
                let placemarks = try await geocoder.reverseGeocodeLocation(loc, preferredLocale: locale)
                let pm = placemarks.first
                self.fetchWeather(
                    lat: loc.coordinate.latitude,
                    lon: loc.coordinate.longitude,
                    iso: pm?.isoCountryCode,
                    country: pm?.country,
                    admin: pm?.administrativeArea,
                    city: pm?.locality ?? pm?.subAdministrativeArea ?? ""
                )
            } catch {
                self.fetchWeather(
                    lat: loc.coordinate.latitude,
                    lon: loc.coordinate.longitude,
                    iso: nil,
                    country: nil,
                    admin: nil,
                    city: ""
                )
            }
        }
    }
    
    func locationManager(_ manager: CLLocationManager, didFailWithError error: Error) {
        failToKyoto()
    }
    
    private func fetchWeather(lat: Double, lon: Double, iso: String?, country: String?, admin: String?, city: String) {
        let urlStr = "https://api.open-meteo.com/v1/forecast?latitude=\(lat)&longitude=\(lon)&current_weather=true"
        guard let url = URL(string: urlStr) else { failToKyoto(); return }
        
        URLSession.shared.dataTask(with: url) { data, _, _ in
            var windSpeed: Double = 0
            var temp: Double = 0
            var weatherCode: Int = 0
            if let data = data,
               let json = try? JSONSerialization.jsonObject(with: data) as? [String: Any],
               let current = json["current_weather"] as? [String: Any] {
                windSpeed = current["windspeed"] as? Double ?? 0
                temp = current["temperature"] as? Double ?? 0
                weatherCode = current["weathercode"] as? Int ?? 0
            }
            self.generateKeywords(
                windSpeed: windSpeed,
                temp: temp,
                weatherCode: weatherCode,
                iso: iso,
                country: country,
                admin: admin,
                city: city
            )
        }.resume()
    }
    
    private func generateKeywords(
        windSpeed: Double,
        temp: Double,
        weatherCode: Int,
        iso: String?,
        country: String?,
        admin: String?,
        city: String
    ) {
        let now = Date()
        let calendar = Calendar.current
        let month = calendar.component(.month, from: now)
        let hour = calendar.component(.hour, from: now)
        
        let season = (3...5).contains(month) ? "Spring" : (6...8).contains(month) ? "Summer" : (9...11).contains(month) ? "Autumn" : "Winter"
        let sunStr: String
        switch hour {
        case 4..<11: sunStr = "Morning"
        case 11..<16: sunStr = "Daytime"
        case 16..<19: sunStr = "Evening"
        default: sunStr = "Night"
        }
        let windStr = windSpeed < 5 ? "Calm" : (windSpeed > 20 ? "Windy" : "Breezy")
        let moonStr = getMoonPhase(date: now)
        var condition = "Clear"
        switch weatherCode {
        case 1...3: condition = "Cloudy"
        case 45, 48: condition = "Fog"
        case 51...67, 80...82: condition = "Rain"
        case 71...77, 85...86: condition = "Snow"
        case 95...99: condition = "Storm"
        default: condition = "Clear"
        }
        
        let isJapanese = currentLanguage == "日本語" || currentLanguage.contains("Japanese")
        let place = PlaceLiteraryLexicon.build(
            isoCountryCode: iso,
            countryName: country,
            adminArea: admin,
            city: city,
            preferJapaneseDisplay: isJapanese
        )
        
        var searchKeywords: [String] = place.search
        
        let sWords = ["Spring": ["Spring", "Blossom", "Flower"], "Summer": ["Summer", "Sun", "Sea"], "Autumn": ["Autumn", "Leaf", "Fall"], "Winter": ["Winter", "Snow", "Ice"]][season] ?? [season]
        let tWords = ["Morning": ["Morning", "Dawn", "Light"], "Daytime": ["Day", "Sunlight", "Sky"], "Evening": ["Evening", "Dusk", "Sunset"], "Night": ["Night", "Dark", "Dream"]][sunStr] ?? [sunStr]
        let wWords = ["Clear": ["Clear", "Sky", "Light"], "Cloudy": ["Cloud", "Gray"], "Rain": ["Rain", "Drop", "Water"], "Snow": ["Snow", "White"], "Fog": ["Fog", "Mist"], "Storm": ["Storm", "Wind"]][condition] ?? [condition]
        
        // 気候・時間は従来どおり足すが、国・土地より後ろ（比重は場所優先）
        searchKeywords.append(sWords[0])
        searchKeywords.append(tWords[0])
        searchKeywords.append(wWords[0])
        searchKeywords = PlaceLiteraryLexicon.uniquePreserve(searchKeywords)
        
        var displaySeason = season
        var displaySun = sunStr
        var displayWind = windStr
        var displayMoon = moonStr
        var displayCond = condition
        
        if isJapanese {
            let sMap = ["Spring": "春", "Summer": "夏", "Autumn": "秋", "Winter": "冬"]
            let sunMap = ["Morning": "朝", "Daytime": "昼", "Evening": "夕暮れ", "Night": "夜"]
            let wMap = ["Calm": "穏やか", "Breezy": "そよ風", "Windy": "風"]
            let mMap = ["New Moon": "新月", "Crescent Moon": "三日月", "First Quarter Moon": "上弦の月", "Full Moon": "満月", "Last Quarter Moon": "下弦の月", "Waning Moon": "有明の月"]
            let cMap = ["Clear": "晴れ", "Cloudy": "曇り", "Rain": "雨", "Snow": "雪", "Fog": "霧", "Storm": "嵐"]
            
            displaySeason = sMap[season] ?? season
            displaySun = sunMap[sunStr] ?? sunStr
            displayWind = wMap[windStr] ?? windStr
            displayMoon = mMap[moonStr] ?? moonStr
            displayCond = cMap[condition] ?? condition
        }
        
        let tempInt = Int(round(temp))
        let placeLabel = place.displayPlace
        var displayString = ""
        
        if isJapanese {
            let prefix = placeLabel.isEmpty ? "" : placeLabel + "\n"
            displayString = prefix + displaySeason + "、" + displaySun + "\n" + displayCond + " / " + displayWind + " / " + displayMoon + "\n(" + String(tempInt) + "°C)"
        } else {
            let prefix = placeLabel.isEmpty ? "" : placeLabel + "\n"
            displayString = prefix + displaySeason + ", " + displaySun + "\n" + displayCond + " / " + displayWind + " / " + displayMoon + "\n(" + String(tempInt) + "°C)"
        }
        
        DispatchQueue.main.async {
            self.isSensing = false
            QuoteDatabase.shared.atmospherePlaceKeywords = Array(place.search.prefix(8))
            self.completion?(searchKeywords, displayString)
        }
    }
    
    private func getMoonPhase(date: Date) -> String {
        let calendar = Calendar.current
        let year = calendar.component(.year, from: date)
        let month = calendar.component(.month, from: date)
        let day = calendar.component(.day, from: date)
        
        var y = year, m = month
        if m < 3 { y -= 1; m += 12 }
        let a = y / 100
        let b = a / 4
        let c = 2 - a + b
        let e = Int(365.25 * Double(y + 4716))
        let f = Int(30.6001 * Double(m + 1))
        let jd = Double(c + day + e + f) - 1524.5
        let daysSinceNew = jd - 2451549.5
        let newMoons = daysSinceNew / 29.53
        let phase = newMoons - floor(newMoons)
        let age = phase * 29.53
        
        if age < 1.84 || age >= 27.68 { return "New Moon" }
        if age < 5.53 { return "Crescent Moon" }
        if age < 12.91 { return "First Quarter Moon" }
        if age < 16.61 { return "Full Moon" }
        if age < 23.99 { return "Last Quarter Moon" }
        return "Waning Moon"
    }
}

class QuoteDatabase: ObservableObject {
    static let shared = QuoteDatabase()
    
    @Published var filteredFortunes: [[String: Any]] = []
    @Published var isFiltering = false
    @Published var currentSearchText = ""
    @Published var totalHitCount: Int = 0
    
    @Published var atmosphereFortunes: [[String: Any]] = []
    @Published var isAtmosphereMode = false
    @Published var currentAtmosphereKeywords: [String] = []
    @Published var totalAtmosphereHitCount: Int = 0
    /// 気配検索で場所を優先ソートするための国・都市キーワード
    var atmospherePlaceKeywords: [String] = []
    
    private var preloadedRandomQuote: [String: Any]? = nil
    private let apiBaseURL = "https://lagado.jp/fragments/api.php"
    
    var roulettePool: [String] = [
        "To be, or not to be...",
        "It was the best of times...",
        "Call me Ishmael.",
        "I am no bird; and no net ensnares me...",
        "All grown-ups were once children..."
    ]
    
    init() {
        preloadNextRandomQuote()
        fillRoulettePoolInitial()
    }
    
    func clearPreloadedQuote() {
        self.preloadedRandomQuote = nil
        self.preloadNextRandomQuote()
    }
    
    private func preloadNextRandomQuote() {
        let mode = UserDefaults.standard.integer(forKey: "quoteLengthMode") == 1 ? "long" : "short"
        guard let url = URL(string: "\(apiBaseURL)?action=random&mode=\(mode)") else { return }
        
        URLSession.shared.dataTask(with: url) { data, _, _ in
            if let data = data, let dict = try? JSONSerialization.jsonObject(with: data) as? [String: Any], dict["error"] == nil {
                self.preloadedRandomQuote = dict
            }
        }.resume()
    }
    
    private func fillRoulettePoolInitial() {
        for i in 0..<5 {
            DispatchQueue.global().asyncAfter(deadline: .now() + Double(i) * 0.8) {
                self.fetchSingleRouletteText()
            }
        }
    }
    
    func replenishRoulettePool() {
        fetchSingleRouletteText()
    }
    
    private func fetchSingleRouletteText() {
        let mode = UserDefaults.standard.integer(forKey: "quoteLengthMode") == 1 ? "long" : "short"
        guard let url = URL(string: "\(apiBaseURL)?action=random&mode=\(mode)") else { return }
        URLSession.shared.dataTask(with: url) { data, _, _ in
            if let data = data,
               let dict = try? JSONSerialization.jsonObject(with: data) as? [String: Any],
               let quote = dict["quote"] as? String {
                DispatchQueue.main.async {
                    if self.roulettePool.count >= 15 {
                        self.roulettePool.removeFirst()
                    }
                    self.roulettePool.append(quote)
                }
            }
        }.resume()
    }
    
    func fetchRandomQuote(completion: @escaping ([String: Any]?) -> Void) {
        if let readyQuote = preloadedRandomQuote {
            DispatchQueue.main.async { completion(readyQuote) }
            self.preloadedRandomQuote = nil
            self.preloadNextRandomQuote()
        } else {
            let mode = UserDefaults.standard.integer(forKey: "quoteLengthMode") == 1 ? "long" : "short"
            guard let url = URL(string: "\(apiBaseURL)?action=random&mode=\(mode)") else { return }
            URLSession.shared.dataTask(with: url) { data, _, _ in
                if let data = data, let dict = try? JSONSerialization.jsonObject(with: data) as? [String: Any], dict["error"] == nil {
                    DispatchQueue.main.async { completion(dict) }
                    self.preloadNextRandomQuote()
                } else {
                    let fallbackQuote: [String: Any] = [
                        "quote": "Words reside in silence. Please connect to the internet to welcome new words. (言葉は静寂の中にあります。インターネットに接続してください)",
                        "title": "Network Error / 通信エラー",
                        "author": "System"
                    ]
                    DispatchQueue.main.async { completion(fallbackQuote) }
                }
            }.resume()
        }
    }
    
    func searchAPI(keyword: String, scope: SearchScope = .all, completion: @escaping ([[String: Any]]) -> Void) {
        let cleanText = keyword.trimmingCharacters(in: .whitespacesAndNewlines)
        if cleanText.isEmpty { clearFilter(); completion([]); return }
        
        var scopeStr = "all"
        switch scope {
        case .quote: scopeStr = "quote"
        case .author: scopeStr = "author"
        case .title: scopeStr = "title"
        default: scopeStr = "all"
        }
        
        let encodedKeyword = cleanText.addingPercentEncoding(withAllowedCharacters: .urlQueryAllowed) ?? ""
        guard let url = URL(string: "\(apiBaseURL)?action=search&keyword=\(encodedKeyword)&scope=\(scopeStr)&mode=both") else { return }
        
        URLSession.shared.dataTask(with: url) { data, _, _ in
            if let data = data, let results = try? JSONSerialization.jsonObject(with: data) as? [[String: Any]] {
                DispatchQueue.main.async {
                    self.isAtmosphereMode = false
                    self.currentAtmosphereKeywords = []
                    self.atmosphereFortunes = []
                    self.totalAtmosphereHitCount = 0
                    self.atmospherePlaceKeywords = []
                    
                    self.isFiltering = true
                    self.currentSearchText = cleanText
                    self.filteredFortunes = results
                    self.totalHitCount = results.count
                    completion(results)
                }
            } else {
                DispatchQueue.main.async { completion([]) }
            }
        }.resume()
    }
    
    private func preferPlaceMatches(_ results: [[String: Any]], placeKeys: [String]) -> [[String: Any]] {
        guard !placeKeys.isEmpty, !results.isEmpty else { return results }
        let keys = placeKeys.map { $0.lowercased() }
        let scored: [(Int, [String: Any])] = results.map { row in
            let hay = [
                String(describing: row["quote"] ?? ""),
                String(describing: row["title"] ?? ""),
                String(describing: row["author"] ?? "")
            ].joined(separator: " ").lowercased()
            let score = keys.reduce(0) { partial, key in
                partial + (hay.contains(key) ? 3 : 0)
            }
            return (score, row)
        }
        let ranked = scored.sorted { $0.0 > $1.0 }
        let strong = ranked.filter { $0.0 > 0 }.map { $0.1 }
        if strong.isEmpty { return results }
        let weak = ranked.filter { $0.0 == 0 }.map { $0.1 }
        return strong + weak
    }

    func fetchQuotesForAtmosphere(keywords: [String], completion: @escaping ([String: Any]?) -> Void) {
        let cleanKeywords = keywords.map { $0.trimmingCharacters(in: .whitespacesAndNewlines) }.filter { !$0.isEmpty }
        let joined = cleanKeywords.joined(separator: ",")
        let encodedKeywords = joined.addingPercentEncoding(withAllowedCharacters: .urlQueryAllowed) ?? ""
        
        let mode = UserDefaults.standard.integer(forKey: "quoteLengthMode") == 1 ? "long" : "short"
        guard let url = URL(string: "\(apiBaseURL)?action=atmosphere&keywords=\(encodedKeywords)&mode=\(mode)") else { return }
        
        URLSession.shared.dataTask(with: url) { data, _, _ in
            if let data = data, let results = try? JSONSerialization.jsonObject(with: data) as? [[String: Any]], !results.isEmpty {
                DispatchQueue.main.async {
                    let ranked = self.preferPlaceMatches(results, placeKeys: self.atmospherePlaceKeywords)
                    self.isAtmosphereMode = true
                    self.currentAtmosphereKeywords = cleanKeywords
                    self.atmosphereFortunes = ranked
                    self.totalAtmosphereHitCount = ranked.count
                    
                    self.isFiltering = false
                    self.currentSearchText = ""
                    self.filteredFortunes = []
                    self.totalHitCount = 0
                    
                    // 場所ヒット優先の先頭群からランダム（全世界対応）
                    let placeBoost = min(ranked.count, max(8, ranked.count / 3))
                    completion(Array(ranked.prefix(placeBoost)).randomElement() ?? ranked.randomElement())
                }
            } else {
                DispatchQueue.main.async { completion(nil) }
            }
        }.resume()
    }
    
    func clearFilter() {
        isFiltering = false; filteredFortunes = []; currentSearchText = ""; totalHitCount = 0
    }
    
    func clearAtmosphere() {
        isAtmosphereMode = false
        currentAtmosphereKeywords = []
        atmosphereFortunes = []
        totalAtmosphereHitCount = 0
        atmospherePlaceKeywords = []
    }
}

class QuoteStorage: ObservableObject {
    static let shared = QuoteStorage()
    
    @Published var favorites: [StockedQuote] = []
    @Published var history: [StockedQuote] = []
    
    private let keyFavorites = "stockedQuotes_v3"
    private let keyHistory = "displayHistory_v3"
    
    init() {
        loadAll()
    }
    
    func loadAll() {
        favorites = _loadFavorites()
        history = _loadHistory()
    }
    
    private func _loadFavorites() -> [StockedQuote] {
        if let data = UserDefaults.standard.data(forKey: keyFavorites),
           let quotes = try? JSONDecoder().decode([StockedQuote].self, from: data) {
            return quotes
        }
        return []
    }
    
    private func saveFavorites() {
        if let data = try? JSONEncoder().encode(favorites) {
            UserDefaults.standard.set(data, forKey: keyFavorites)
        }
    }
    
    func addFavorite(text: String, title: String, author: String) {
        let cleanText = text.trimmingCharacters(in: .whitespacesAndNewlines)
        if !favorites.contains(where: { $0.text.trimmingCharacters(in: .whitespacesAndNewlines) == cleanText }) {
            favorites.insert(StockedQuote(text: cleanText, title: title, author: author, date: Date()), at: 0)
            saveFavorites()
        }
    }
    
    func removeFavorite(text: String) {
        let cleanText = text.trimmingCharacters(in: .whitespacesAndNewlines)
        favorites.removeAll { $0.text.trimmingCharacters(in: .whitespacesAndNewlines) == cleanText }
        saveFavorites()
    }

    private func _loadHistory() -> [StockedQuote] {
        guard let data = UserDefaults.standard.data(forKey: keyHistory),
              let quotes = try? JSONDecoder().decode([StockedQuote].self, from: data) else { return [] }
        return quotes
    }
    
    private func saveHistory() {
        if let data = try? JSONEncoder().encode(history) {
            UserDefaults.standard.set(data, forKey: keyHistory)
        }
    }
    
    func addHistory(text: String, title: String, author: String) {
        let cleanText = text.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !cleanText.isEmpty else { return }
        
        history.removeAll { $0.text.trimmingCharacters(in: .whitespacesAndNewlines) == cleanText }
        history.insert(StockedQuote(text: cleanText, title: title, author: author, date: Date()), at: 0)
        if history.count > 100 { history = Array(history.prefix(100)) }
        saveHistory()
    }
    
    func removeHistory(text: String) {
        let cleanText = text.trimmingCharacters(in: .whitespacesAndNewlines)
        history.removeAll { $0.text.trimmingCharacters(in: .whitespacesAndNewlines) == cleanText }
        saveHistory()
    }
}

class LanguageManager: ObservableObject {
    @Published var ui: UIStrings = .defaultEnglish()
    @Published var isTranslating = false
    @AppStorage("nativeLanguage") var nativeLanguage = "English"

    let allLanguages: [String] = {
        let majorCodes = [
            "en", "ja", "zh", "es", "fr", "de", "ko", "it", "pt", "ru",
            "ar", "hi", "id", "tr", "vi", "th", "nl", "pl", "sv", "fi",
            "da", "no", "cs", "el", "hu", "ro", "uk", "ms", "he", "fa"
        ]
        var names: [String] = []
        for code in majorCodes {
            let locale = Locale(identifier: code)
            if let name = locale.localizedString(forLanguageCode: code) {
                names.append(name.capitalized)
            }
        }
        return names
    }()

    init() { loadUI(for: nativeLanguage) }

    func setLanguage(_ lang: String) {
        nativeLanguage = lang
        loadUI(for: lang)
    }

    func loadUI(for language: String) {
        if let data = UserDefaults.standard.data(forKey: "ui_strings_v5_\(language)"),
           let cached = try? JSONDecoder().decode(UIStrings.self, from: data) {
            self.ui = cached; return
        }
        self.ui = .defaultEnglish()
        if language.lowercased().contains("english") { return }
        fetchTranslation(for: language)
    }

    private func fetchTranslation(for targetLanguage: String) {
        DispatchQueue.main.async { self.isTranslating = true }
        
        let isJp = targetLanguage == "日本語" || targetLanguage.contains("Japanese")
        var prompt = "Translate the following UI strings into " + targetLanguage + ".\n"
        if isJp {
            prompt += "CRITICAL RULE for Japanese translation:\n"
            prompt += "- 'Draw a Quote' MUST be translated EXACTLY as '言葉を引く'. Do NOT use '名言'.\n"
            prompt += "- 'Swipe horizontally to draw a new quote.' MUST be translated as 'スワイプして新しい言葉を引く'.\n"
            prompt += "- 'AI Explanation Level' MUST be translated EXACTLY as 'AI解説のレベル'.\n"
            prompt += "- 'Display History' MUST be translated EXACTLY as '閲覧履歴'.\n"
            prompt += "- 'Discover approx. 43 million fragments extracted from 60,000 books, displayed at random. Save your favorites and use AI explanations to deepen your understanding.' MUST be translated EXACTLY as '6万冊以上の名著から抽出した約4,300万の言葉の断片をランダムに表示します。お気に入りを保存し、AI解説を活用して深く味わいましょう。'\n"
            prompt += "- 'Quote Length' MUST be translated EXACTLY as '文章の長さ'.\n"
            prompt += "- 'Short' MUST be translated EXACTLY as '短文'.\n"
            prompt += "- 'Long' MUST be translated EXACTLY as '長文'.\n"
            prompt += "- 'A ticket to unlock AI explanations.' MUST be translated EXACTLY as 'AI解説を読むためのチケットです。'\n"
        }
        prompt += "Return ONLY a valid JSON object. Do NOT include markdown formatting or backticks.\n"
        prompt += "{ \"settingsTitle\": \"Settings\", \"nativeLanguageTitle\": \"Your Native Language\", \"aiLevelTitle\": \"AI Explanation Level\", \"done\": \"Done\", \"ticketStore\": \"Ticket Store\", \"selectLanguage\": \"Select your language\", \"stockTitle\": \"Stocked Quotes\", \"emptyStock\": \"No quotes stocked yet.\", \"viewExplanation\": \"View Explanation\", \"close\": \"Close\", \"aiExplanationTitle\": \"AI Explanation\", \"generatingText\": \"Generating...\\nPlease wait⏳\", \"outOfTicketsTitle\": \"Out of Tickets\", \"outOfTicketsMsg\": \"You need tickets for AI Explanation.\", \"buyButton\": \"Go to Store\", \"cancel\": \"Cancel\", \"status\": \"Status\", \"remainingTickets\": \"Tickets\", \"level1\": \"Middle School\", \"level2\": \"High School\", \"level3\": \"College\", \"level4\": \"Business\", \"level1Desc\": \"Avoids difficult grammar terms and explains basic sentence structures gently. Great for beginners.\", \"level2Desc\": \"Points out important grammar for exams and explains logical sentence structures.\", \"level3Desc\": \"Explores literary metaphors, nuances, and cultural backgrounds for advanced learners.\", \"level4Desc\": \"Focuses on formality and how to use expressions in practical professional situations.\", \"confirmExplanationTitle\": \"Confirm\", \"confirmExplanationMsg\": \"Use 1 ticket to generate an AI explanation?\", \"generateButton\": \"Generate\", \"onboardingTitle\": \"Welcome to Fragments\", \"onboardingDesc\": \"Discover approx. 43 million fragments extracted from 60,000 books, displayed at random. Save your favorites and use AI explanations to deepen your understanding.\", \"nextButton\": \"Next\", \"startButton\": \"Start\", \"tutorialFeaturesTitle\": \"How to Use\", \"feature1Title\": \"Translate Words\", \"feature1Desc\": \"Tap any word to use the built-in translation.\", \"feature2Title\": \"Search Web\", \"feature2Desc\": \"Tap the author or title to search the book on the web.\", \"feature3Title\": \"Draw a Quote\", \"feature3Desc\": \"Swipe horizontally to draw a new quote.\", \"databaseSearch\": \"Database Search\", \"searchPlaceholder\": \"Search by keyword...\", \"searchEmpty\": \"No results found.\", \"historyTitle\": \"Display History\", \"emptyHistory\": \"No history yet.\", \"quoteLengthTitle\": \"Quote Length\", \"lengthShort\": \"Short\", \"lengthLong\": \"Long\", \"ticketDesc\": \"A ticket to unlock AI explanations.\" }"

        guard let url = URL(string: "https://lagado.jp/fragments/gemini.php") else { return }
        let requestBody: [String: Any] = ["contents": [["parts": [["text": prompt]]]]]
        guard let httpBody = try? JSONSerialization.data(withJSONObject: requestBody) else { return }

        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.httpBody = httpBody

        URLSession.shared.dataTask(with: request) { data, _, _ in
            DispatchQueue.main.async {
                self.isTranslating = false
                
                guard let data = data,
                      let json = try? JSONSerialization.jsonObject(with: data) as? [String: Any],
                      let candidates = json["candidates"] as? [[String: Any]],
                      let firstCandidate = candidates.first,
                      let content = firstCandidate["content"] as? [String: Any],
                      let parts = content["parts"] as? [[String: Any]],
                      let firstPart = parts.first,
                      let text = firstPart["text"] as? String else {
                    return
                }

                let marker = String(repeating: "`", count: 3)
                let cleanText = text.replacingOccurrences(of: "\(marker)json", with: "").replacingOccurrences(of: marker, with: "").trimmingCharacters(in: .whitespacesAndNewlines)

                if let textData = cleanText.data(using: .utf8), let translatedUI = try? JSONDecoder().decode(UIStrings.self, from: textData) {
                    self.ui = translatedUI
                    if let encoded = try? JSONEncoder().encode(translatedUI) { UserDefaults.standard.set(encoded, forKey: "ui_strings_v5_\(targetLanguage)") }
                }
            }
        }.resume()
    }
}

struct FeatureRow: View {
    let icon: String
    let iconColor: Color
    let title: String
    let desc: String
    
    var body: some View {
        HStack(spacing: 20) {
            Image(systemName: icon).font(.system(size: 28)).foregroundColor(iconColor).frame(width: 35)
            VStack(alignment: .leading, spacing: 4) {
                Text(title).font(.headline).foregroundColor(.primary)
                Text(desc).font(.subheadline).foregroundColor(.secondary).fixedSize(horizontal: false, vertical: true)
            }
        }
    }
}

struct OnboardingView: View {
    @ObservedObject var langManager: LanguageManager
    @Binding var englishLevelIndex: Int
    @AppStorage("hasCompletedOnboarding") var hasCompletedOnboarding = false
    @State private var currentStep = 0

    var body: some View {
        ZStack {
            Color(UIColor.systemGroupedBackground).edgesIgnoringSafeArea(.all)
            
            VStack {
                if currentStep == 0 {
                    VStack(spacing: 15) {
                        Spacer()
                        ZStack {
                            Circle().fill(Color.orange.opacity(0.1)).frame(width: 120, height: 120)
                            Image(systemName: "globe").font(.system(size: 60)).foregroundColor(.orange)
                        }.padding(.bottom, 10)
                        Text("Choose Your Language").font(.title2).bold()
                        Text("言語を選択してください / 选择您的语言").font(.caption).foregroundColor(.secondary)
                        
                        List {
                            ForEach(langManager.allLanguages, id: \.self) { lang in
                                Button(action: {
                                    UIImpactFeedbackGenerator(style: .medium).impactOccurred()
                                    langManager.setLanguage(lang)
                                    if !langManager.isTranslating {
                                        DispatchQueue.main.asyncAfter(deadline: .now() + 0.3) { withAnimation { currentStep = 1 } }
                                    }
                                }) {
                                    HStack {
                                        Text(lang).font(.title3).foregroundColor(.primary)
                                        Spacer()
                                        if langManager.nativeLanguage == lang { Image(systemName: "checkmark").foregroundColor(.orange) }
                                    }.padding(.vertical, 12)
                                }
                            }
                        }
                        .listStyle(InsetGroupedListStyle())
                        .frame(maxHeight: 350)
                        Spacer()
                    }
                } else if currentStep == 1 {
                    Spacer()
                    Image(systemName: "book.pages").font(.system(size: 80)).foregroundColor(.orange).padding()
                    Text(langManager.ui.onboardingTitle).font(.title).bold().multilineTextAlignment(.center).padding()
                    Text(langManager.ui.onboardingDesc).font(.body).multilineTextAlignment(.center).foregroundColor(.secondary).padding(.horizontal, 30)
                    Spacer()
                    
                    Button(action: {
                        UIImpactFeedbackGenerator(style: .light).impactOccurred(); withAnimation { currentStep = 2 }
                    }) {
                        Text(langManager.ui.nextButton).font(.headline).foregroundColor(.white).frame(maxWidth: .infinity).padding().background(Color.orange).cornerRadius(12)
                    }.padding(.horizontal, 40).padding(.bottom, 50)
                    
                } else if currentStep == 2 {
                    VStack(spacing: 20) {
                        Text(langManager.ui.tutorialFeaturesTitle).font(.title2).bold().padding(.top, 20)
                        Image("TutorialImage").resizable().scaledToFit().frame(maxHeight: 380).cornerRadius(16).shadow(color: Color.black.opacity(0.15), radius: 8, x: 0, y: 4).padding(.horizontal, 25)
                        
                        VStack(alignment: .leading, spacing: 20) {
                            FeatureRow(icon: "hand.tap.fill", iconColor: .blue, title: langManager.ui.feature1Title, desc: langManager.ui.feature1Desc)
                            FeatureRow(icon: "safari.fill", iconColor: .green, title: langManager.ui.feature2Title, desc: langManager.ui.feature2Desc)
                            FeatureRow(icon: "arrow.left.and.right", iconColor: .orange, title: langManager.ui.feature3Title, desc: langManager.ui.feature3Desc)
                        }.padding(.horizontal, 35).padding(.top, 10)
                        Spacer()
                        
                        Button(action: {
                            UIImpactFeedbackGenerator(style: .light).impactOccurred(); withAnimation { currentStep = 3 }
                        }) {
                            Text(langManager.ui.nextButton).font(.headline).foregroundColor(.white).frame(maxWidth: .infinity).padding().background(Color.orange).cornerRadius(12)
                        }.padding(.horizontal, 40).padding(.bottom, 40)
                    }
                } else if currentStep == 3 {
                    let levels = [langManager.ui.level1, langManager.ui.level2, langManager.ui.level3, langManager.ui.level4]
                    let descList = [langManager.ui.level1Desc, langManager.ui.level2Desc, langManager.ui.level3Desc, langManager.ui.level4Desc]
                    
                    Spacer()
                    Text(langManager.ui.aiLevelTitle).font(.title).fontWeight(.light).padding(.bottom, 10)
                    
                    Picker(langManager.ui.aiLevelTitle, selection: $englishLevelIndex) {
                        ForEach(0..<levels.count, id: \.self) { index in Text(levels[index]).tag(index) }
                    }.pickerStyle(WheelPickerStyle()).frame(height: 150).padding(.horizontal)
                    
                    let safeIndex = max(0, min(englishLevelIndex, descList.count - 1))
                    Text(descList[safeIndex])
                        .font(.subheadline)
                        .foregroundColor(.secondary)
                        .multilineTextAlignment(.center)
                        .padding(.horizontal, 30)
                        .frame(minHeight: 80, alignment: .top)
                    
                    Spacer()
                    
                    Button(action: {
                        UIImpactFeedbackGenerator(style: .medium).impactOccurred()
                        withAnimation { hasCompletedOnboarding = true }
                    }) {
                        Text(langManager.ui.startButton).font(.headline).foregroundColor(.white).frame(maxWidth: .infinity).padding().background(Color.orange).cornerRadius(12)
                    }.padding(.horizontal, 40).padding(.bottom, 50)
                }
            }
            
            if langManager.isTranslating {
                Color.black.opacity(0.6).edgesIgnoringSafeArea(.all)
                VStack(spacing: 20) {
                    ProgressView().scaleEffect(2.0).colorInvert()
                    Text("Translating UI via Gemini...").font(.headline).foregroundColor(.white)
                }
            }
        }
        .onChange(of: langManager.isTranslating) { oldValue, newValue in
            if !newValue && currentStep == 0 { withAnimation { currentStep = 1 } }
        }
    }
}

struct SettingsView: View {
    @Environment(\.presentationMode) var presentationMode
    @ObservedObject var langManager: LanguageManager
    @Binding var englishLevelIndex: Int
    @ObservedObject var ticketManager = TicketManager.shared

    @State private var searchText = ""
    @State private var selectedScope: SearchScope = .all
    @State private var showNoResultsAlert = false
    @State private var isSearching = false
    
    @State private var showAtmosphereAlert = false
    @State private var sensedText = ""
    @State private var sensedKeywords: [String] = []
    
    @State private var isFetchingAtmosphere = false
    
    @StateObject private var atmosphere = AtmosphereManager.shared
    @ObservedObject private var db = QuoteDatabase.shared
    
    @AppStorage("themePreference") var themePreference = 0
    @AppStorage("quoteLengthMode") var quoteLengthMode = 0

    func drawRandomQuote() {
        UIImpactFeedbackGenerator(style: .medium).impactOccurred()
        db.clearFilter()
        db.clearAtmosphere()
        searchText = ""
        
        db.fetchRandomQuote { quoteDict in
            if let quote = quoteDict {
                NotificationCenter.default.post(name: .forceDisplayQuote, object: quote)
                self.presentationMode.wrappedValue.dismiss()
            }
        }
    }

    var body: some View {
        let ui = langManager.ui
        let levels = [ui.level1, ui.level2, ui.level3, ui.level4]
        let descList = [ui.level1Desc, ui.level2Desc, ui.level3Desc, ui.level4Desc]
        
        let isJapanese = langManager.nativeLanguage == "日本語" || langManager.nativeLanguage.contains("Japanese")
        let drawSectionTitle = isJapanese ? "言葉を引く" : "Draw a Quote"
        let randomButtonText = isJapanese ? "無作為に引く" : "Draw Randomly"
        let senseButtonText = isJapanese ? "今の気配を読み取る" : "Sense the Moment"
        let searchSectionTitle = isJapanese ? "言葉を探す" : "Search"
        
        ZStack {
            NavigationView {
                Form {
                    // 1. 言葉を引く
                    Section(header: Text(drawSectionTitle)) {
                        Button(action: {
                            drawRandomQuote()
                        }) {
                            HStack {
                                Image(systemName: "dice.fill").foregroundColor(.purple)
                                Text(randomButtonText).foregroundColor(.primary)
                            }
                        }
                        
                        Button(action: {
                            UIImpactFeedbackGenerator(style: .medium).impactOccurred()
                            atmosphere.senseMoment(language: langManager.nativeLanguage) { keywords, displayString in
                                self.sensedKeywords = keywords
                                self.sensedText = displayString
                                withAnimation(.spring()) {
                                    self.showAtmosphereAlert = true
                                }
                            }
                        }) {
                            HStack {
                                if atmosphere.isSensing {
                                    ProgressView().padding(.trailing, 4)
                                } else {
                                    Image(systemName: "sparkles").foregroundColor(.orange)
                                }
                                Text(atmosphere.isSensing ? (isJapanese ? "読み取り中..." : "Sensing...") : senseButtonText)
                                    .foregroundColor(atmosphere.isSensing ? .gray : .primary)
                            }
                        }
                        .disabled(atmosphere.isSensing)
                        
                        if db.isAtmosphereMode {
                            Button(action: {
                                db.clearAtmosphere()
                                UIImpactFeedbackGenerator(style: .medium).impactOccurred()
                            }) {
                                HStack {
                                    Image(systemName: "xmark.circle")
                                    Text(isJapanese ? "気配モードを解除" : "Clear Atmosphere")
                                }
                                .foregroundColor(.orange)
                            }
                        }
                    }
                    
                    // 2. 言葉を探す
                    Section(header: Text(searchSectionTitle)) {
                        Picker("Search Scope", selection: $selectedScope) {
                            ForEach(SearchScope.allCases, id: \.self) { scope in
                                Text(scope.localizedName(isJapanese: isJapanese)).tag(scope)
                            }
                        }
                        .pickerStyle(SegmentedPickerStyle())
                        .padding(.vertical, 4)
                        
                        HStack {
                            Image(systemName: "magnifyingglass").foregroundColor(.gray)
                            TextField(ui.searchPlaceholder, text: $searchText)
                                .submitLabel(.search)
                                .onSubmit { performSearch() }
                                .disabled(isSearching)
                                .onChange(of: searchText) { oldValue, newValue in
                                    if newValue.isEmpty && db.isFiltering {
                                        db.clearFilter()
                                    }
                                }
                            
                            if isSearching {
                                ProgressView()
                            }
                            if !isSearching && !searchText.isEmpty {
                                Button(action: {
                                    searchText = ""
                                    db.clearFilter()
                                    UIImpactFeedbackGenerator(style: .light).impactOccurred()
                                }) {
                                    Image(systemName: "xmark.circle.fill").foregroundColor(.gray)
                                }
                            }
                        }
                        
                        if db.isFiltering {
                            Button(action: {
                                db.clearFilter()
                                searchText = ""
                                UIImpactFeedbackGenerator(style: .medium).impactOccurred()
                            }) {
                                HStack {
                                    Image(systemName: "xmark.circle")
                                    Text(isJapanese ? "検索状態を解除" : "Clear Search")
                                }
                                .foregroundColor(.red)
                            }
                        }
                    }
                    
                    // 3. AI解説のレベル
                    let safeIndex = max(0, min(englishLevelIndex, descList.count - 1))
                    Section(header: Text(ui.aiLevelTitle), footer: Text(descList[safeIndex])) {
                        Picker(ui.aiLevelTitle, selection: $englishLevelIndex) {
                            ForEach(0..<levels.count, id: \.self) { index in Text(levels[index]).tag(index) }
                        }
                    }
                    
                    // 4. 文章の長さ
                    Section(header: Text(ui.quoteLengthTitle)) {
                        HStack(spacing: 0) {
                            Button(action: {
                                if quoteLengthMode == 0 {
                                    drawRandomQuote()
                                } else {
                                    quoteLengthMode = 0
                                    UIImpactFeedbackGenerator(style: .light).impactOccurred()
                                    db.clearPreloadedQuote()
                                }
                            }) {
                                Text(ui.lengthShort)
                                    .font(.subheadline)
                                    .fontWeight(quoteLengthMode == 0 ? .semibold : .regular)
                                    .frame(maxWidth: .infinity)
                                    .padding(.vertical, 6)
                                    .background(quoteLengthMode == 0 ? Color(UIColor.systemBackground) : Color.clear)
                                    .cornerRadius(7)
                                    .shadow(color: quoteLengthMode == 0 ? Color.black.opacity(0.12) : Color.clear, radius: 2, x: 0, y: 1)
                            }
                            .foregroundColor(quoteLengthMode == 0 ? .primary : .gray)
                            
                            Button(action: {
                                if quoteLengthMode == 1 {
                                    drawRandomQuote()
                                } else {
                                    quoteLengthMode = 1
                                    UIImpactFeedbackGenerator(style: .light).impactOccurred()
                                    db.clearPreloadedQuote()
                                }
                            }) {
                                Text(ui.lengthLong)
                                    .font(.subheadline)
                                    .fontWeight(quoteLengthMode == 1 ? .semibold : .regular)
                                    .frame(maxWidth: .infinity)
                                    .padding(.vertical, 6)
                                    .background(quoteLengthMode == 1 ? Color(UIColor.systemBackground) : Color.clear)
                                    .cornerRadius(7)
                                    .shadow(color: quoteLengthMode == 1 ? Color.black.opacity(0.12) : Color.clear, radius: 2, x: 0, y: 1)
                            }
                            .foregroundColor(quoteLengthMode == 1 ? .primary : .gray)
                        }
                        .padding(3)
                        .background(Color(UIColor.tertiarySystemFill))
                        .cornerRadius(9)
                        .buttonStyle(PlainButtonStyle())
                    }
                    
                    // 5. 外観 (テーマ)
                    Section(header: Text(isJapanese ? "外観 (テーマ)" : "Appearance")) {
                        Picker(isJapanese ? "テーマを選択" : "Theme", selection: $themePreference) {
                            Text(isJapanese ? "端末に合わせる" : "System").tag(0)
                            Text(isJapanese ? "ライト" : "Light").tag(1)
                            Text(isJapanese ? "ナイト" : "Night").tag(2)
                        }
                        .pickerStyle(SegmentedPickerStyle())
                        .onChange(of: themePreference) { oldValue, newValue in
                            NotificationCenter.default.post(name: NSNotification.Name("ThemeChanged"), object: newValue)
                            UIApplication.shared.connectedScenes
                                .compactMap { $0 as? UIWindowScene }
                                .flatMap { $0.windows }
                                .forEach { window in
                                    window.overrideUserInterfaceStyle = newValue == 1 ? .light : (newValue == 2 ? .dark : .unspecified)
                                }
                        }
                    }
                    
                    // 6. 母国語
                    Section(header: Text(ui.nativeLanguageTitle)) {
                        Picker(ui.nativeLanguageTitle, selection: Binding(get: { langManager.nativeLanguage }, set: { langManager.setLanguage($0) })) {
                            ForEach(langManager.allLanguages, id: \.self) { lang in Text(lang).tag(lang) }
                        }
                    }
                    
                    // 7. チケットストア
                    Section(header: Text(ui.ticketStore)) {
                        HStack {
                            Text(isJapanese ? "本日の無料解説" : "Free Today")
                            Spacer()
                            if isJapanese {
                                Text("\(ticketManager.freeTickets) 回").fontWeight(.bold).foregroundColor(.green)
                            } else {
                                Text("\(ticketManager.freeTickets)").fontWeight(.bold).foregroundColor(.green)
                            }
                        }
                        HStack {
                            Text(isJapanese ? "所有チケット" : "Paid Tickets")
                            Spacer()
                            if isJapanese {
                                Text("\(ticketManager.paidTickets) 枚").fontWeight(.bold)
                            } else {
                                Text("\(ticketManager.paidTickets)").fontWeight(.bold)
                            }
                        }
                        NavigationLink(destination: TicketStoreView(langManager: langManager)) {
                            Text("\(ui.buyButton) ›").foregroundColor(.blue).fontWeight(.bold)
                        }
                    }
                    
                    // 8. プライバシーポリシー
                    Section {
                        if let url = URL(string: "https://lagado.jp/fragments/privacy.php") {
                            Link(destination: url) {
                                HStack {
                                    Text(isJapanese ? "プライバシーポリシー" : "Privacy Policy")
                                        .foregroundColor(.primary)
                                    Spacer()
                                    Image(systemName: "arrow.up.right.square")
                                        .foregroundColor(.gray)
                                }
                            }
                        }
                    }
                    
                }
                .navigationBarTitle(ui.settingsTitle, displayMode: .inline)
                .navigationBarItems(trailing: Button(ui.done) { presentationMode.wrappedValue.dismiss() })
                .onAppear {
                    ticketManager.checkDailyReset()
                    if db.isFiltering {
                        searchText = db.currentSearchText
                    } else {
                        searchText = ""
                    }
                }
                .alert(isPresented: $showNoResultsAlert) {
                    Alert(title: Text(isJapanese ? "見つかりませんでした" : "Not Found"), message: Text(isJapanese ? "別のキーワードをお試しください。" : "Please try another keyword."), dismissButton: .default(Text("OK")))
                }
            }
            
            if showAtmosphereAlert {
                Color.black.opacity(0.45)
                    .edgesIgnoringSafeArea(.all)
                    .onTapGesture {
                        if !isFetchingAtmosphere {
                            withAnimation(.spring()) { showAtmosphereAlert = false }
                        }
                    }
                
                VStack(spacing: 20) {
                    Text(isJapanese ? "今の気配" : "Current Atmosphere")
                        .font(.headline)
                        .padding(.top, 25)
                    
                    Text(sensedText)
                        .font(.system(size: 22, weight: .medium, design: .serif))
                        .multilineTextAlignment(.center)
                        .lineSpacing(8)
                        .padding(.horizontal, 20)
                        .padding(.vertical, 10)
                    
                    Text(isJapanese ? "この気配に重なる言葉を引きますか？" : "Draw a quote matching this moment?")
                        .font(.subheadline)
                        .foregroundColor(.secondary)
                        .padding(.bottom, 15)
                    
                    HStack(spacing: 0) {
                        Button(action: {
                            withAnimation(.spring()) { showAtmosphereAlert = false }
                        }) {
                            Text(isJapanese ? "キャンセル" : "Cancel")
                                .fontWeight(.medium)
                                .frame(maxWidth: .infinity)
                                .padding()
                                .foregroundColor(.primary)
                        }
                        .disabled(isFetchingAtmosphere)
                        
                        Divider()
                            .frame(height: 50)
                        
                        Button(action: {
                            isFetchingAtmosphere = true
                            performAtmosphereSearch()
                        }) {
                            if isFetchingAtmosphere {
                                ProgressView()
                                    .progressViewStyle(CircularProgressViewStyle(tint: .orange))
                                    .frame(maxWidth: .infinity)
                                    .padding()
                            } else {
                                Text(isJapanese ? "言葉を引く" : "Draw Quote")
                                    .fontWeight(.bold)
                                    .foregroundColor(.orange)
                                    .frame(maxWidth: .infinity)
                                    .padding()
                            }
                        }
                        .disabled(isFetchingAtmosphere)
                    }
                    .overlay(Divider(), alignment: .top)
                }
                .background(Color(UIColor.systemBackground))
                .cornerRadius(18)
                .shadow(color: .black.opacity(0.2), radius: 25, x: 0, y: 15)
                .padding(.horizontal, 40)
                .transition(.scale(scale: 0.9).combined(with: .opacity))
            }
        }
    }
    
    func performSearch() {
        guard !searchText.isEmpty else { return }
        UIImpactFeedbackGenerator(style: .medium).impactOccurred()
        
        db.clearAtmosphere()
        
        isSearching = true
        db.searchAPI(keyword: searchText, scope: selectedScope) { results in
            self.isSearching = false
            
            if results.isEmpty {
                self.showNoResultsAlert = true
            } else {
                if let quote = results.randomElement() {
                    NotificationCenter.default.post(name: .forceDisplayQuote, object: quote)
                    self.presentationMode.wrappedValue.dismiss()
                }
            }
        }
    }
    
    func performAtmosphereSearch() {
        guard !sensedKeywords.isEmpty else {
            isFetchingAtmosphere = false
            withAnimation(.spring()) { showAtmosphereAlert = false }
            return
        }
        
        db.fetchQuotesForAtmosphere(keywords: sensedKeywords) { targetQuote in
            if let quote = targetQuote {
                NotificationCenter.default.post(name: .forceDisplayQuote, object: quote)
                self.isFetchingAtmosphere = false
                self.presentationMode.wrappedValue.dismiss()
            } else {
                self.db.fetchRandomQuote { randomDict in
                    if let dict = randomDict {
                        NotificationCenter.default.post(name: .forceDisplayQuote, object: dict)
                    }
                    self.isFetchingAtmosphere = false
                    self.presentationMode.wrappedValue.dismiss()
                }
            }
        }
    }
}

struct TicketStoreView: View {
    @ObservedObject var langManager: LanguageManager
    @Environment(\.presentationMode) var presentationMode
    @StateObject private var storeManager = StoreManager()
    @ObservedObject var ticketManager = TicketManager.shared

    var body: some View {
        let isJapanese = langManager.nativeLanguage == "日本語" || langManager.nativeLanguage.contains("Japanese")
        Form {
            Section {
                HStack {
                    Spacer()
                    VStack(spacing: 8) {
                        Image(systemName: "ticket.fill")
                            .font(.system(size: 40))
                            .foregroundColor(.orange)
                        Text("\(ticketManager.paidTickets)")
                            .font(.system(size: 40, weight: .bold, design: .rounded))
                        Text(isJapanese ? "所有チケット" : "Paid Tickets")
                            .font(.caption)
                            .foregroundColor(.secondary)
                    }
                    .padding(.vertical, 20)
                    Spacer()
                }
            } footer: {
                Text(langManager.ui.outOfTicketsMsg)
                    .multilineTextAlignment(.center)
                    .frame(maxWidth: .infinity)
            }
            
            Section(header: Text("Store")) {
                if storeManager.isLoadingProducts {
                    HStack {
                        Spacer()
                        ProgressView()
                        Spacer()
                    }
                } else if storeManager.products.isEmpty {
                    Text(isJapanese ? "商品情報を取得できませんでした。通信環境を確認してください。" : "Could not load products. Please check your connection.")
                        .font(.subheadline)
                        .foregroundColor(.secondary)
                        .padding(.vertical, 8)
                } else {
                    ForEach(storeManager.products) { product in
                        RealTicketPackRow(product: product, descriptionText: langManager.ui.ticketDesc, isPurchasing: storeManager.isPurchasing) {
                            Task { try? await storeManager.purchase(product) }
                        }
                    }
                }
            }
        }
        .navigationBarTitle(langManager.ui.ticketStore, displayMode: .inline)
        .onAppear {
            ticketManager.checkDailyReset()
        }
    }
}

struct TicketPackRow: View {
    let amount: Int
    let price: String
    let badge: String?
    var action: () -> Void
    
    var body: some View {
        Button(action: {
            UIImpactFeedbackGenerator(style: .medium).impactOccurred()
            action()
        }) {
            HStack {
                VStack(alignment: .leading, spacing: 4) {
                    if let badge = badge {
                        Text(badge)
                            .font(.system(size: 10, weight: .bold))
                            .tracking(1.0)
                            .foregroundColor(.orange)
                    }
                    HStack(alignment: .firstTextBaseline, spacing: 4) {
                        Text("\(amount)")
                            .font(.title3)
                            .fontWeight(.semibold)
                            .foregroundColor(.primary)
                        Text("Tickets")
                            .font(.subheadline)
                            .foregroundColor(.secondary)
                    }
                }
                
                Spacer()
                
                Text(price)
                    .font(.subheadline)
                    .fontWeight(.bold)
                    .foregroundColor(.white)
                    .padding(.vertical, 6)
                    .padding(.horizontal, 16)
                    .background(Color.blue)
                    .cornerRadius(16)
            }
            .padding(.vertical, 4)
        }
    }
}

struct RealTicketPackRow: View {
    let product: Product
    let descriptionText: String
    let isPurchasing: Bool
    var action: () -> Void
    
    var body: some View {
        Button(action: {
            UIImpactFeedbackGenerator(style: .medium).impactOccurred()
            action()
        }) {
            HStack {
                VStack(alignment: .leading, spacing: 4) {
                    HStack(alignment: .firstTextBaseline, spacing: 4) {
                        Text(product.displayName)
                            .font(.title3)
                            .fontWeight(.semibold)
                            .foregroundColor(.primary)
                    }
                    Text(descriptionText)
                        .font(.caption)
                        .foregroundColor(.secondary)
                }
                
                Spacer()
                
                if isPurchasing {
                    ProgressView()
                        .padding(.vertical, 6)
                        .padding(.horizontal, 16)
                } else {
                    Text(product.displayPrice)
                        .font(.subheadline)
                        .fontWeight(.bold)
                        .foregroundColor(.white)
                        .padding(.vertical, 6)
                        .padding(.horizontal, 16)
                        .background(Color.blue)
                        .cornerRadius(16)
                }
            }
            .padding(.vertical, 4)
        }
        .disabled(isPurchasing)
    }
}

struct ContentView: View {
    enum ActiveAlert: Identifiable {
        case outOfTickets
        case confirmExplanation
        var id: Int { hashValue }
    }

    @StateObject var langManager = LanguageManager()
    @StateObject var ticketManager = TicketManager.shared
    
    @AppStorage("hasCompletedOnboarding") var hasCompletedOnboarding = false

    @State private var showingSettings = false
    @State private var showingExplanation = false
    @State private var showingFavorites = false
    @State private var activeAlert: ActiveAlert?
    
    @State private var currentQuoteForAI = ""
    @State private var quoteDictToForceDisplay: [String: Any]? = nil
    
    @AppStorage("englishLevelIndex") var englishLevelIndex = 1
    @AppStorage("themePreference") var themePreference = 0
    @Environment(\.colorScheme) var colorScheme
    @Environment(\.scenePhase) var scenePhase

    @State private var showNativeTranslation = false
    @State private var wordToTranslate = ""

    var body: some View {
        ZStack {
            Group {
                if themePreference == 1 {
                    Color(red: 0.98, green: 0.96, blue: 0.92)
                } else if themePreference == 2 {
                    Color(UIColor.systemBackground)
                } else {
                    (colorScheme == .dark ? Color(UIColor.systemBackground) : Color(red: 0.98, green: 0.96, blue: 0.92))
                }
            }
            .edgesIgnoringSafeArea(.all)

            WebView(
                showingSettings: $showingSettings,
                showingExplanation: $showingExplanation,
                showingFavorites: $showingFavorites,
                currentQuote: $currentQuoteForAI,
                activeAlert: $activeAlert,
                showNativeTranslation: $showNativeTranslation,
                wordToTranslate: $wordToTranslate,
                quoteDictToForceDisplay: $quoteDictToForceDisplay,
                themePreference: themePreference,
                nativeLanguage: langManager.nativeLanguage,
                ui: langManager.ui
            )
            .edgesIgnoringSafeArea(.all)
        }
        .onAppear {
            ticketManager.checkDailyReset()
            _ = QuoteDatabase.shared
            
            UIApplication.shared.connectedScenes
                .compactMap { $0 as? UIWindowScene }
                .flatMap { $0.windows }
                .forEach { window in
                    window.overrideUserInterfaceStyle = themePreference == 1 ? .light : (themePreference == 2 ? .dark : .unspecified)
                }
        }
        .onReceive(NotificationCenter.default.publisher(for: .forceDisplayQuote)) { notification in
            if let dict = notification.object as? [String: Any] {
                quoteDictToForceDisplay = dict
            } else if let text = notification.object as? String {
                quoteDictToForceDisplay = ["quote": text]
            }
        }
        .onReceive(NotificationCenter.default.publisher(for: .closeSettings)) { _ in showingSettings = false }
        .fullScreenCover(isPresented: Binding(get: { !hasCompletedOnboarding }, set: { _ in })) { OnboardingView(langManager: langManager, englishLevelIndex: $englishLevelIndex) }
        .sheet(isPresented: $showingSettings) { SettingsView(langManager: langManager, englishLevelIndex: $englishLevelIndex) }
        .sheet(isPresented: $showingExplanation) { ExplanationView(quote: $currentQuoteForAI, langManager: langManager, englishLevelIndex: $englishLevelIndex) }
        .sheet(isPresented: $showingFavorites) { FavoritesView(langManager: langManager, currentQuoteForAI: $currentQuoteForAI, showingExplanation: $showingExplanation) }
        .translationPresentation(isPresented: $showNativeTranslation, text: wordToTranslate)
        .alert(item: $activeAlert) { alertType in
            switch alertType {
            case .outOfTickets:
                return Alert(
                    title: Text(langManager.ui.outOfTicketsTitle),
                    message: Text(langManager.ui.outOfTicketsMsg),
                    primaryButton: .default(Text(langManager.ui.buyButton)) { showingSettings = true },
                    secondaryButton: .cancel(Text(langManager.ui.cancel))
                )
            case .confirmExplanation:
                return Alert(
                    title: Text(langManager.ui.confirmExplanationTitle),
                    message: Text(langManager.ui.confirmExplanationMsg),
                    primaryButton: .default(Text(langManager.ui.generateButton)) {
                        if ticketManager.consumeTicket() {
                            var explainedQuotes = UserDefaults.standard.stringArray(forKey: "explainedQuotes") ?? []
                            if !explainedQuotes.contains(currentQuoteForAI) {
                                explainedQuotes.insert(currentQuoteForAI, at: 0)
                                UserDefaults.standard.set(explainedQuotes, forKey: "explainedQuotes")
                            }
                            showingExplanation = true
                        } else {
                            activeAlert = .outOfTickets
                        }
                    },
                    secondaryButton: .cancel(Text(langManager.ui.cancel))
                )
            }
        }
        .onChange(of: scenePhase) { oldPhase, newPhase in
            if newPhase == .active {
                UIApplication.shared.connectedScenes
                    .compactMap { $0 as? UIWindowScene }
                    .flatMap { $0.windows }
                    .forEach { window in
                        window.overrideUserInterfaceStyle = themePreference == 1 ? .light : (themePreference == 2 ? .dark : .unspecified)
                    }
                NotificationCenter.default.post(name: NSNotification.Name("ThemeChanged"), object: themePreference)
            }
        }
        .onChange(of: themePreference) { oldValue, newValue in
            NotificationCenter.default.post(name: NSNotification.Name("ThemeChanged"), object: newValue)
        }
        .onChange(of: colorScheme) { oldValue, newValue in
            NotificationCenter.default.post(name: NSNotification.Name("ThemeChanged"), object: themePreference)
        }
    }
}

struct FavoritesView: View {
    @Environment(\.presentationMode) var presentationMode
    @ObservedObject var langManager: LanguageManager
    @Binding var currentQuoteForAI: String
    @Binding var showingExplanation: Bool
    
    @State private var selectedTab = 0
    @ObservedObject var storage = QuoteStorage.shared

    var body: some View {
        NavigationView {
            VStack(spacing: 0) {
                HStack(spacing: 0) {
                    Button(action: {
                        UIImpactFeedbackGenerator(style: .light).impactOccurred()
                        withAnimation(.easeInOut(duration: 0.2)) { selectedTab = 0 }
                    }) {
                        Image(systemName: "star.fill")
                            .font(.system(size: 18))
                            .foregroundColor(selectedTab == 0 ? .orange : .gray)
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 6)
                            .background(selectedTab == 0 ? Color(UIColor.systemBackground) : Color.clear)
                            .cornerRadius(7)
                            .shadow(color: selectedTab == 0 ? Color.black.opacity(0.12) : Color.clear, radius: 2, x: 0, y: 1)
                    }
                    
                    Button(action: {
                        UIImpactFeedbackGenerator(style: .light).impactOccurred()
                        withAnimation(.easeInOut(duration: 0.2)) { selectedTab = 1 }
                    }) {
                        Image(systemName: "clock.fill")
                            .font(.system(size: 18))
                            .foregroundColor(selectedTab == 1 ? .primary : .gray)
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 6)
                            .background(selectedTab == 1 ? Color(UIColor.systemBackground) : Color.clear)
                            .cornerRadius(7)
                            .shadow(color: selectedTab == 1 ? Color.black.opacity(0.12) : Color.clear, radius: 2, x: 0, y: 1)
                    }
                }
                .padding(3)
                .background(Color(UIColor.tertiarySystemGroupedBackground))
                .cornerRadius(9)
                .padding(.horizontal)
                .padding(.vertical, 12)
                .background(Color(UIColor.systemGroupedBackground))
                
                QuoteStockListView(langManager: langManager, presentationMode: presentationMode, currentQuoteForAI: $currentQuoteForAI, showingExplanation: $showingExplanation, items: selectedTab == 0 ? storage.favorites : storage.history, isFavoritesTab: selectedTab == 0)
            }
            .navigationBarTitle(selectedTab == 0 ? langManager.ui.stockTitle : langManager.ui.historyTitle, displayMode: .inline)
            .navigationBarItems(trailing: Button(langManager.ui.close) { presentationMode.wrappedValue.dismiss() })
        }
    }
}

struct QuoteStockListView: View {
    @ObservedObject var langManager: LanguageManager
    var presentationMode: Binding<PresentationMode>
    @Binding var currentQuoteForAI: String
    @Binding var showingExplanation: Bool
    
    let items: [StockedQuote]
    let isFavoritesTab: Bool

    var groupedItems: [(String, [StockedQuote])] {
        let formatter = DateFormatter(); formatter.setLocalizedDateFormatFromTemplate("yMMMM")
        let grouped = Dictionary(grouping: items) { formatter.string(from: $0.date) }
        return grouped.sorted { a, b in (a.value.first?.date ?? .distantPast) > (b.value.first?.date ?? .distantPast) }
    }

    var body: some View {
        List {
            if items.isEmpty {
                Text(isFavoritesTab ? langManager.ui.emptyStock : langManager.ui.emptyHistory).foregroundColor(.gray).padding()
            } else {
                ForEach(groupedItems, id: \.0) { month, quotes in
                    Section(header: Text(month)) {
                        ForEach(quotes, id: \.text) { item in
                            VStack(alignment: .leading, spacing: 10) {
                                Button(action: {
                                    let dict: [String: Any] = [
                                        "quote": item.text,
                                        "title": item.title ?? "",
                                        "author": item.author ?? "",
                                        "skipRoulette": true
                                    ]
                                    NotificationCenter.default.post(name: .forceDisplayQuote, object: dict)
                                    presentationMode.wrappedValue.dismiss()
                                }) {
                                    VStack(alignment: .leading, spacing: 6) {
                                        Text(item.text).font(.body).foregroundColor(.primary).multilineTextAlignment(.leading)
                                        if let t = item.title, let a = item.author, (!t.isEmpty || !a.isEmpty) {
                                            Text("\(t) / \(a)").font(.caption).foregroundColor(.secondary)
                                        }
                                    }
                                }.buttonStyle(PlainButtonStyle())
                                
                                if UserDefaults.standard.data(forKey: "chatHistory_\(item.text)") != nil || UserDefaults.standard.dictionary(forKey: "quoteExplanations")?[item.text] != nil {
                                    Button(action: {
                                        currentQuoteForAI = item.text; presentationMode.wrappedValue.dismiss()
                                        DispatchQueue.main.asyncAfter(deadline: .now() + 0.5) { showingExplanation = true }
                                    }) { HStack { Image(systemName: "sparkles"); Text(langManager.ui.viewExplanation) }.font(.caption).foregroundColor(.blue).padding(.horizontal, 12).padding(.vertical, 6).background(Color.blue.opacity(0.1)).cornerRadius(8) }.buttonStyle(PlainButtonStyle())
                                }
                            }
                            .padding(.vertical, 8)
                            .swipeActions(edge: .trailing, allowsFullSwipe: true) { if isFavoritesTab { Button(role: .destructive, action: { deleteItem(text: item.text) }) { Label("Delete", systemImage: "trash") } } }
                        }
                    }
                }
            }
        }
    }
    func deleteItem(text: String) { if isFavoritesTab { QuoteStorage.shared.removeFavorite(text: text) } }
}

struct MessageBubble: View {
    let msg: ChatMessage
    
    var body: some View {
        HStack(alignment: .bottom) {
            if msg.isUser {
                Spacer()
                Text(msg.text)
                    .font(.body)
                    .lineSpacing(6)
                    .padding()
                    .background(Color.blue.opacity(0.1))
                    .cornerRadius(12)
                    .foregroundColor(.blue)
            } else {
                Text(msg.text)
                    .font(.body)
                    .lineSpacing(6)
                    .padding()
                    .background(Color(uiColor: .secondarySystemBackground))
                    .cornerRadius(12)
                    .foregroundColor(.primary)
                Spacer()
            }
        }
    }
}

struct ExplanationView: View {
    @Binding var quote: String
    @ObservedObject var langManager: LanguageManager
    @Binding var englishLevelIndex: Int
    @ObservedObject var ticketManager = TicketManager.shared

    @State private var messages: [ChatMessage] = []
    @State private var inputText: String = ""
    @State private var isLoading = true
    @Environment(\.presentationMode) var presentationMode

    var inputPlaceholder: String {
        let isJapanese = langManager.nativeLanguage == "日本語" || langManager.nativeLanguage.contains("Japanese")
        if ticketManager.freeTickets > 0 {
            return isJapanese ? "質問を入力... (無料枠: 残り\(ticketManager.freeTickets)回)" : "Ask a question... (Free: \(ticketManager.freeTickets) left)"
        } else {
            return isJapanese ? "質問を入力... (チケット: 残り\(ticketManager.paidTickets)枚)" : "Ask a question... (Tickets: \(ticketManager.paidTickets) left)"
        }
    }

    var body: some View {
        NavigationView {
            VStack(spacing: 0) {
                ScrollViewReader { proxy in
                    ScrollView {
                        VStack(alignment: .leading, spacing: 20) {
                            Color.clear.frame(height: 1).id("top")
                            Text("「\(quote)」").font(.headline).italic().padding(.top, 15)
                            
                            ForEach(messages) { msg in
                                MessageBubble(msg: msg)
                            }
                            
                            if isLoading { HStack { Spacer(); VStack(spacing: 10) { ProgressView().scaleEffect(1.2); Text(langManager.ui.generatingText).font(.caption).foregroundColor(.gray) }; Spacer() }.padding() }
                            Color.clear.frame(height: 1).id("bottom")
                        }.padding(.horizontal)
                    }
                    .onChange(of: messages) { oldValue, newValue in
                        if newValue.count <= 1 { withAnimation { proxy.scrollTo("top", anchor: .top) } } else { withAnimation { proxy.scrollTo("bottom", anchor: .bottom) } }
                    }
                    .onChange(of: isLoading) { oldValue, newValue in
                        if newValue && messages.count > 0 { withAnimation { proxy.scrollTo("bottom", anchor: .bottom) } }
                    }
                }
                Divider()
                
                HStack {
                    TextField(inputPlaceholder, text: $inputText)
                        .textFieldStyle(RoundedBorderTextFieldStyle())
                        .disabled(isLoading)
                    
                    Button(action: {
                        let textToSend = inputText; inputText = ""; sendFollowUpRequest(text: textToSend)
                    }) {
                        Image(systemName: "paperplane.fill")
                            .foregroundColor(inputText.isEmpty || isLoading ? .gray : .blue)
                            .padding(.horizontal, 8)
                    }
                    .disabled(inputText.isEmpty || isLoading)
                }
                .padding()
                .background(Color(UIColor.systemBackground))
            }
            .navigationBarTitle(langManager.ui.aiExplanationTitle, displayMode: .inline)
            .navigationBarItems(trailing: Button(langManager.ui.close) { presentationMode.wrappedValue.dismiss() })
            .onAppear { loadInitialData() }
        }
    }

    func loadInitialData() {
        if let data = UserDefaults.standard.data(forKey: "chatHistory_\(quote)"), let savedMessages = try? JSONDecoder().decode([ChatMessage].self, from: data) { self.messages = savedMessages; self.isLoading = false; return }
        if let explanations = UserDefaults.standard.dictionary(forKey: "quoteExplanations") as? [String: String], let oldText = explanations[quote] { self.messages = [ChatMessage(isUser: false, text: oldText)]; saveHistory(); self.isLoading = false; return }
        requestGemini(isInitial: true)
    }
    
    func sendFollowUpRequest(text: String) {
        if ticketManager.consumeTicket() {
            UIImpactFeedbackGenerator(style: .medium).impactOccurred()
            messages.append(ChatMessage(isUser: true, text: text))
            requestGemini(isInitial: false)
        } else {
            NotificationCenter.default.post(name: NSNotification.Name("ShowOutOfTicketsAlert"), object: nil)
        }
    }
    
    func requestGemini(isInitial: Bool) {
        isLoading = true
        
        guard let url = URL(string: "https://lagado.jp/fragments/gemini.php") else { return }
        
        let targetLang = langManager.nativeLanguage
        let isJapanese = targetLang == "日本語" || targetLang.contains("Japanese")
        
        var initialPrompt = ""
        
        if isJapanese {
            let jpLevels = [
                "中学英語の基礎（英検3〜4級程度）を前提に、複雑な文法用語は避け、基本的な構文（SVOなど）や基礎単語をわかりやすくやさしい言葉で解説してください。",
                "高校英語（英検準2〜2級、大学受験レベル）を前提に、関係詞、分詞構文、仮定法などの重要文法を指摘し、文の構造を論理的に解説してください。",
                "大学生・教養レベル（英検準1級以上）を前提に、文学的な比喩やニュアンス、文化的背景、抽象的な語彙の深掘りを含めて、よりアカデミックで高度な解説を行ってください。",
                "ビジネスパーソンを前提に、この表現や含まれる単語が実際のビジネスシーン（メール、会議、交渉など）でどう活かせるか、フォーマル度やプロフェッショナルな言い回しに焦点を当てて解説してください。"
            ]
            let specificInstruction = jpLevels[max(0, min(englishLevelIndex, jpLevels.count - 1))]
            
            initialPrompt += "あなたはプロの英語教師であり、文学コンシェルジュでもあります。客観的かつ簡潔に出力してください。\n"
            initialPrompt += "【重要】挨拶、前置き、結びの言葉は一切不要です。いきなり「【作品と作家】」から出力を開始してください。Markdown記号は使用せず、プレーンテキストで見やすく整形してください。\n\n"
            initialPrompt += "対象読者のレベルと解説方針：\n"
            initialPrompt += specificInstruction + "\n\n"
            initialPrompt += "【文章】 \"" + quote + "\"\n\n"
            initialPrompt += "初回解説時は以下の6つの角度から出力してください。\n"
            initialPrompt += "1. 【作品と作家】 (この文章の出典作品、著者名、およびその簡単な紹介や時代背景)\n"
            initialPrompt += "2. 【和訳】 (直訳に近い正確な意味)\n"
            initialPrompt += "3. 【意訳】 (自然で美しい、文学的な日本語表現)\n"
            initialPrompt += "4. 【語彙・文法】 (対象レベルに合わせた重要単語や構文の解説)\n"
            initialPrompt += "5. 【ニュアンス】 (言葉の裏にある感情や背景)\n"
            initialPrompt += "6. 【実践・応用】 (対象レベルに合わせた、短い英語の例文を1つ添える)"
        } else {
            let enLevels = [
                "Explain gently using basic grammar and simple words, suitable for middle school level beginners.",
                "Point out important grammar points and logically explain the sentence structure, suitable for high school/college prep level.",
                "Provide advanced explanations including literary metaphors, nuances, and cultural background, suitable for college level.",
                "Focus on the formality and how to use these expressions in practical business situations."
            ]
            let specificInstruction = enLevels[max(0, min(englishLevelIndex, enLevels.count - 1))]
            
            initialPrompt += "You are a professional language teacher and literary concierge. Output objectively and concisely.\n"
            initialPrompt += "[IMPORTANT] Do NOT include any greetings, introductions, or closing remarks. Start your output directly from '[Author & Work]'. Do not use Markdown symbols like *, _, or #. Format it as clean plain text.\n\n"
            initialPrompt += "Target audience level and explanation policy:\n"
            initialPrompt += specificInstruction + "\n\n"
            initialPrompt += "Target Language for Explanation: " + targetLang + "\n"
            initialPrompt += "(You MUST output your entire response in " + targetLang + ")\n\n"
            initialPrompt += "[Quote] \"" + quote + "\"\n\n"
            initialPrompt += "For this initial explanation, please output from the following 6 angles:\n"
            initialPrompt += "1. [Author & Work] (Source work, author name, brief introduction, and historical background)\n"
            initialPrompt += "2. [Literal Translation] (Accurate meaning close to literal translation)\n"
            initialPrompt += "3. [Literary Translation] (Natural, beautiful, and literary translation)\n"
            initialPrompt += "4. [Vocabulary & Grammar] (Explanation of important words and syntax tailored to the target level)\n"
            initialPrompt += "5. [Nuance] (Emotions and background behind the words)\n"
            initialPrompt += "6. [Practical Usage] (Provide one short example sentence tailored to the target level)"
        }
        
        var contents: [[String: Any]] = [["role": "user", "parts": [["text": initialPrompt]]]]
        for msg in messages { contents.append(["role": msg.isUser ? "user" : "model", "parts": [["text": msg.text]]]) }
        guard let httpBody = try? JSONSerialization.data(withJSONObject: ["contents": contents]) else { return }
        
        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.httpBody = httpBody
        
        URLSession.shared.dataTask(with: request) { data, response, error in
            DispatchQueue.main.async {
                self.isLoading = false
                
                if let error = error {
                    var errMsg = ""
                    if isJapanese {
                        errMsg = "通信エラー: " + error.localizedDescription + "\nチケットを返還しました。"
                    } else {
                        errMsg = "Network Error: " + error.localizedDescription + "\nTicket refunded."
                    }
                    self.messages.append(ChatMessage(isUser: false, text: errMsg))
                    if !isInitial {
                        TicketManager.shared.freeTickets += 1
                    }
                    return
                }
                
                guard let data = data,
                      let json = try? JSONSerialization.jsonObject(with: data) as? [String: Any],
                      let candidates = json["candidates"] as? [[String: Any]],
                      let firstCandidate = candidates.first,
                      let content = firstCandidate["content"] as? [String: Any],
                      let parts = content["parts"] as? [[String: Any]],
                      let firstPart = parts.first,
                      let text = firstPart["text"] as? String else {
                    
                    let parseErr = isJapanese ? "AIからの応答を解析できませんでした。" : "Failed to parse AI response."
                    self.messages.append(ChatMessage(isUser: false, text: parseErr))
                    if !isInitial {
                        TicketManager.shared.freeTickets += 1
                    }
                    return
                }
                
                self.messages.append(ChatMessage(isUser: false, text: text))
                self.saveHistory()
            }
        }.resume()
    }
    
    func saveHistory() { if let data = try? JSONEncoder().encode(messages) { UserDefaults.standard.set(data, forKey: "chatHistory_\(quote)") } }
}

class WeakScriptMessageHandlerDelegate: NSObject, WKScriptMessageHandler {
    weak var delegate: WKScriptMessageHandler?
    init(_ delegate: WKScriptMessageHandler) { self.delegate = delegate; super.init() }
    func userContentController(_ userContentController: WKUserContentController, didReceive message: WKScriptMessage) { delegate?.userContentController(userContentController, didReceive: message) }
}

struct WebView: UIViewRepresentable {
    @Binding var showingSettings: Bool
    @Binding var showingExplanation: Bool
    @Binding var showingFavorites: Bool
    @Binding var currentQuote: String
    @Binding var activeAlert: ContentView.ActiveAlert?
    @Binding var showNativeTranslation: Bool
    @Binding var wordToTranslate: String
    @Binding var quoteDictToForceDisplay: [String: Any]?
    
    var themePreference: Int
    var nativeLanguage: String
    var ui: UIStrings

    func makeUIView(context: Context) -> WKWebView {
        let contentController = WKUserContentController()
        let names = ["speakText", "triggerHaptic", "showSettings", "showFavorites", "explainQuote", "stockQuote", "unstockQuote", "requestNextQuote", "requestPreviousQuote", "searchBook", "showNativeTranslation"]
        for name in names { contentController.add(WeakScriptMessageHandlerDelegate(context.coordinator), name: name) }
        let config = WKWebViewConfiguration(); config.userContentController = contentController; config.preferences.setValue(true, forKey: "allowFileAccessFromFileURLs"); config.setValue(true, forKey: "allowUniversalAccessFromFileURLs")
        let webView = WKWebView(frame: .zero, configuration: config); webView.scrollView.bounces = false; webView.isOpaque = false; webView.backgroundColor = .clear;
        
        webView.navigationDelegate = context.coordinator
        
        context.coordinator.webView = webView
        
        var hideUIJS = "var style = document.createElement('style'); "
        hideUIJS += "style.id = 'startup-hide-style'; "
        hideUIJS += "style.innerHTML = '.ripple-circle, #swipe-guide { opacity: 0 !important; pointer-events: none !important; transition: opacity 1.5s ease-in-out !important; }'; "
        hideUIJS += "document.head.appendChild(style);"
        
        let script = WKUserScript(source: hideUIJS, injectionTime: .atDocumentEnd, forMainFrameOnly: true)
        contentController.addUserScript(script)
        
        if let url = Bundle.main.url(forResource: "index", withExtension: "html"),
           let htmlString = try? String(contentsOf: url, encoding: .utf8) {
            webView.loadHTMLString(htmlString, baseURL: Bundle.main.bundleURL)
        } else {
            print("🚨 エラー: index.html がアプリ内に見つかりません！Target Membershipなどを確認してください。")
        }
        
        return webView
    }

    func updateUIView(_ uiView: WKWebView, context: Context) {
        context.coordinator.parent = self
        if let dict = quoteDictToForceDisplay {
            context.coordinator.quoteHistory.append(dict)
            context.coordinator.currentHistoryIndex = context.coordinator.quoteHistory.count - 1
            
            if dict["skipRoulette"] as? Bool == true {
                context.coordinator.sendQuoteToJS(dict: dict)
            } else {
                context.coordinator.startRoulette(finalDict: dict)
            }
            DispatchQueue.main.async { quoteDictToForceDisplay = nil }
        }
        
        let isSystemDark = UITraitCollection.current.userInterfaceStyle == .dark
        let isAppDark = themePreference == 2 || (themePreference == 0 && isSystemDark)
        
        var themeJS = ""
        if isAppDark {
            themeJS = "document.documentElement.setAttribute('data-theme', 'dark');"
        } else {
            themeJS = "document.documentElement.removeAttribute('data-theme');"
        }
        uiView.evaluateJavaScript(themeJS, completionHandler: nil)
    }
    
    static func dismantleUIView(_ uiView: WKWebView, coordinator: Coordinator) {
        let names = ["speakText", "triggerHaptic", "showSettings", "showFavorites", "explainQuote", "stockQuote", "unstockQuote", "requestNextQuote", "requestPreviousQuote", "searchBook", "showNativeTranslation"]
        for name in names { uiView.configuration.userContentController.removeScriptMessageHandler(forName: name) }
    }

    func makeCoordinator() -> Coordinator { Coordinator(self) }

    class Coordinator: NSObject, WKScriptMessageHandler, AVSpeechSynthesizerDelegate, WKNavigationDelegate {
        var parent: WebView; weak var webView: WKWebView?; let speechSynthesizer = AVSpeechSynthesizer()
        var quoteHistory: [[String: Any]] = []; var currentHistoryIndex = -1; var textQueue: [String] = []
        var isPlayingQuote = false
        
        var isSpinning = false
        var rouletteTimer: Timer?
        var isReadyToSwipe = false
        
        let defaultVoice = AVSpeechSynthesisVoice(language: "en-US")
        
        lazy var lightHaptic = UIImpactFeedbackGenerator(style: .light)
        lazy var heavyHaptic = UIImpactFeedbackGenerator(style: .heavy)
        lazy var rigidHaptic = UIImpactFeedbackGenerator(style: .rigid)

        init(_ parent: WebView) {
            self.parent = parent
            super.init()
            speechSynthesizer.delegate = self
            
            NotificationCenter.default.addObserver(forName: NSNotification.Name("ThemeChanged"), object: nil, queue: .main) { [weak self] notification in
                if let newTheme = notification.object as? Int {
                    self?.applyThemeJS(theme: newTheme)
                }
            }
            
            DispatchQueue.main.asyncAfter(deadline: .now() + 2.0) {
                let audioSession = AVAudioSession.sharedInstance()
                try? audioSession.setCategory(.playback, mode: .spokenAudio, options: [.mixWithOthers])
                try? audioSession.setActive(true)
                
                let warmupUtterance = AVSpeechUtterance(string: " ")
                warmupUtterance.volume = 0.0
                self.speechSynthesizer.speak(warmupUtterance)
            }
        }

        deinit {
            NotificationCenter.default.removeObserver(self)
        }
        
        func applyThemeJS(theme: Int) {
            let isSystemDark = UITraitCollection.current.userInterfaceStyle == .dark
            let isAppDark = theme == 2 || (theme == 0 && isSystemDark)
            
            var themeJS = ""
            if isAppDark {
                themeJS = "document.documentElement.setAttribute('data-theme', 'dark');"
            } else {
                themeJS = "document.documentElement.removeAttribute('data-theme');"
            }
            self.webView?.evaluateJavaScript(themeJS, completionHandler: nil)
        }

        func webView(_ webView: WKWebView, didFinish navigation: WKNavigation!) {
            applyThemeJS(theme: parent.themePreference)
            
            for _ in 0..<3 { self.lightHaptic.prepare() }
            var preWarmJS = "try { var tempD = document.getElementById('quote-text'); "
            preWarmJS += "if (tempD) { let origOpacity = tempD.style.opacity; tempD.style.opacity = '0'; "
            preWarmJS += "for(let i=0; i<15; i++) { tempD.innerHTML = 'warmup ' + i; tempD.offsetHeight; } "
            preWarmJS += "tempD.innerHTML = ''; tempD.style.opacity = origOpacity; } } catch(e) {}"
            
            webView.evaluateJavaScript(preWarmJS, completionHandler: nil)
            
            DispatchQueue.main.asyncAfter(deadline: .now() + 1.5) {
                self.isReadyToSwipe = true
                var revealJS = "var hideStyle = document.getElementById('startup-hide-style'); "
                revealJS += "if (hideStyle) { hideStyle.remove(); } "
                revealJS += "document.querySelectorAll('.ripple-circle, #swipe-guide').forEach(el => { el.style.opacity = '1'; el.style.pointerEvents = 'auto'; });"
                
                webView.evaluateJavaScript(revealJS, completionHandler: nil)
            }
        }
        
        private func escapeForJS(_ str: String) -> String {
            return str
                .replacingOccurrences(of: "\\", with: "\\\\")
                .replacingOccurrences(of: "'", with: "\\'")
                .replacingOccurrences(of: "\n", with: "\\n")
                .replacingOccurrences(of: "\r", with: "")
                .replacingOccurrences(of: "\u{2028}", with: "")
                .replacingOccurrences(of: "\u{2029}", with: "")
        }

        func userContentController(_ userContentController: WKUserContentController, didReceive message: WKScriptMessage) {
            switch message.name {
            case "triggerHaptic":
                DispatchQueue.main.async {
                    self.lightHaptic.impactOccurred()
                }
            case "showNativeTranslation":
                if let word = message.body as? String {
                    DispatchQueue.main.async {
                        self.parent.wordToTranslate = word
                        self.parent.showNativeTranslation = true
                    }
                }
            case "requestNextQuote", "requestPreviousQuote":
                // 左右スワイプとも新しいランダム（履歴めくりはしない）
                if !self.isReadyToSwipe { return }
                if self.isSpinning { return }
                
                clearSpeechQueue()
                
                if QuoteDatabase.shared.isFiltering && !QuoteDatabase.shared.filteredFortunes.isEmpty {
                    if let dict = QuoteDatabase.shared.filteredFortunes.randomElement() {
                        self.quoteHistory.append(dict)
                        self.currentHistoryIndex = self.quoteHistory.count - 1
                        self.startRoulette(finalDict: dict)
                    }
                } else if QuoteDatabase.shared.isAtmosphereMode && !QuoteDatabase.shared.atmosphereFortunes.isEmpty {
                    if let dict = QuoteDatabase.shared.atmosphereFortunes.randomElement() {
                        self.quoteHistory.append(dict)
                        self.currentHistoryIndex = self.quoteHistory.count - 1
                        self.startRoulette(finalDict: dict)
                    }
                } else {
                    QuoteDatabase.shared.fetchRandomQuote { [weak self] quoteDict in
                        guard let self = self, let dict = quoteDict else { return }
                        self.quoteHistory.append(dict)
                        self.currentHistoryIndex = self.quoteHistory.count - 1
                        self.startRoulette(finalDict: dict)
                    }
                }
            case "showSettings":
                DispatchQueue.main.async {
                    self.parent.showingSettings = true
                }
            case "showFavorites":
                DispatchQueue.main.async {
                    self.parent.showingFavorites = true
                }
            case "stockQuote", "unstockQuote":
                if let quoteText = message.body as? String {
                    DispatchQueue.main.async {
                        let cleanQuote = quoteText.trimmingCharacters(in: .whitespacesAndNewlines)
                        let isFav = QuoteStorage.shared.favorites.contains(where: { $0.text.trimmingCharacters(in: .whitespacesAndNewlines) == cleanQuote })
                        
                        if message.name == "unstockQuote" {
                            if isFav {
                                QuoteStorage.shared.removeFavorite(text: cleanQuote)
                                self.rigidHaptic.impactOccurred()
                            }
                        } else {
                            if !isFav {
                                if let found = QuoteStorage.shared.history.first(where: { $0.text.trimmingCharacters(in: .whitespacesAndNewlines) == cleanQuote }) {
                                    QuoteStorage.shared.addFavorite(text: cleanQuote, title: found.title ?? "", author: found.author ?? "")
                                } else {
                                    QuoteStorage.shared.addFavorite(text: cleanQuote, title: "", author: "")
                                }
                                UIImpactFeedbackGenerator(style: .medium).impactOccurred()
                            }
                        }
                    }
                }
            case "explainQuote":
                if let quote = message.body as? String {
                    DispatchQueue.main.async {
                        self.parent.currentQuote = quote
                        
                        let isAlreadyExplained = UserDefaults.standard.data(forKey: "chatHistory_\(quote)") != nil || UserDefaults.standard.dictionary(forKey: "quoteExplanations")?[quote] != nil
                        if isAlreadyExplained {
                            var explainedQuotes = UserDefaults.standard.stringArray(forKey: "explainedQuotes") ?? []
                            if !explainedQuotes.contains(quote) {
                                explainedQuotes.insert(quote, at: 0)
                                UserDefaults.standard.set(explainedQuotes, forKey: "explainedQuotes")
                            }
                            self.parent.showingExplanation = true
                        } else {
                            if TicketManager.shared.freeTickets > 0 || TicketManager.shared.paidTickets > 0 {
                                self.parent.activeAlert = .confirmExplanation
                            } else {
                                self.parent.activeAlert = .outOfTickets
                            }
                        }
                    }
                }
            case "speakText":
                if let text = message.body as? String {
                    if self.isPlayingQuote {
                        self.clearSpeechQueue()
                        return
                    }
                    
                    if self.speechSynthesizer.isSpeaking {
                        self.speechSynthesizer.stopSpeaking(at: .immediate)
                    }
                    
                    self.isPlayingQuote = true
                    self.toggleAudioIcon(isPlaying: true)
                    
                    if text.contains("|||") {
                        self.textQueue = text.components(separatedBy: "|||").map { $0.trimmingCharacters(in: .whitespacesAndNewlines) }.filter { !$0.isEmpty }
                    } else if text.contains(" . . . ") {
                        self.textQueue = text.components(separatedBy: " . . . ").map { $0.trimmingCharacters(in: .whitespacesAndNewlines) }.filter { !$0.isEmpty }
                    } else {
                        self.textQueue = [text.trimmingCharacters(in: .whitespacesAndNewlines)]
                    }
                    
                    DispatchQueue.global(qos: .userInitiated).async {
                        try? AVAudioSession.sharedInstance().setActive(true)
                        DispatchQueue.main.async {
                            self.playNextInQueue()
                        }
                    }
                }
            case "searchBook":
                if let dict = message.body as? [String: Any] {
                    let title = dict["title"] as? String ?? ""
                    let author = dict["author"] as? String ?? ""
                    let query = "\(title) \(author)".addingPercentEncoding(withAllowedCharacters: .urlQueryAllowed) ?? ""
                    if let url = URL(string: "https://www.google.com/search?q=\(query)") {
                        DispatchQueue.main.async {
                            UIApplication.shared.open(url)
                        }
                    }
                }
            default:
                break
            }
        }

        func startRoulette(finalDict: [String: Any]) {
            DispatchQueue.main.async {
                self.isSpinning = true
                self.rouletteTimer?.invalidate()
                var ticks = 0
                let maxTicks = 15
                
                self.lightHaptic.prepare()
                self.heavyHaptic.prepare()
                
                var currentSpinTexts = QuoteDatabase.shared.roulettePool.shuffled()
                if currentSpinTexts.isEmpty {
                    currentSpinTexts = ["..."]
                }
                
                QuoteDatabase.shared.replenishRoulettePool()
                
                var initJS = "try { "
                initJS += "var display = document.getElementById('quote-text'); "
                initJS += "var sourceArea = document.getElementById('source-area'); "
                initJS += "var swipeGuide = document.getElementById('swipe-guide'); "
                initJS += "if(display) { display.style.transition = 'none'; display.style.opacity = '1'; display.classList.remove('fade-in'); } "
                initJS += "if(sourceArea) { sourceArea.style.transition = 'none'; sourceArea.style.opacity = '0'; sourceArea.classList.remove('fade-in'); } "
                initJS += "if(swipeGuide) { swipeGuide.style.opacity = '0'; setTimeout(() => { swipeGuide.style.display = 'none'; }, 600); } "
                initJS += "} catch(e) {}"
                
                self.webView?.evaluateJavaScript(initJS, completionHandler: nil)
                
                self.rouletteTimer = Timer.scheduledTimer(withTimeInterval: 0.08, repeats: true) { [weak self] timer in
                    guard let self = self else {
                        timer.invalidate()
                        return
                    }
                    
                    if ticks >= maxTicks {
                        timer.invalidate()
                        self.heavyHaptic.impactOccurred()
                        self.sendQuoteToJS(dict: finalDict)
                        
                        DispatchQueue.main.asyncAfter(deadline: .now() + 0.3) {
                            self.isSpinning = false
                        }
                    } else {
                        let textIndex = ticks % currentSpinTexts.count
                        let text = currentSpinTexts[textIndex]
                        let escapedText = self.escapeForJS(text)
                        
                        var tickJS = "try { var d = document.getElementById('quote-text'); "
                        tickJS += "if(d) { d.innerHTML = '" + escapedText + "'; } } catch(e) {}"
                        
                        self.webView?.evaluateJavaScript(tickJS, completionHandler: nil)
                        
                        if ticks % 2 == 0 {
                            self.lightHaptic.impactOccurred()
                        }
                        ticks += 1
                    }
                }
            }
        }

        func playNextInQueue() {
            guard !textQueue.isEmpty else { return }
            let nextText = textQueue.removeFirst()
            let utterance = AVSpeechUtterance(string: nextText)
            utterance.voice = self.defaultVoice
            utterance.rate = 0.45
            speechSynthesizer.speak(utterance)
        }
        
        func clearSpeechQueue() {
            textQueue.removeAll()
            isPlayingQuote = false
            if speechSynthesizer.isSpeaking {
                speechSynthesizer.stopSpeaking(at: .immediate)
            }
            toggleAudioIcon(isPlaying: false)
        }

        func sendQuoteToJS(dict: [String: Any]) {
            let text = (dict["quote"] as? String) ?? (dict["text"] as? String) ?? (dict["content"] as? String) ?? ""
            let title = (dict["title"] as? String) ?? (dict["book"] as? String) ?? ""
            let author = (dict["author"] as? String) ?? ""
            
            var keyword = ""
            if QuoteDatabase.shared.isFiltering {
                keyword = QuoteDatabase.shared.currentSearchText
            }

            DispatchQueue.main.async {
                if !text.isEmpty {
                    QuoteStorage.shared.addHistory(text: text, title: title, author: author)
                }
                
                let escapedText = self.escapeForJS(text)
                let escapedTitle = self.escapeForJS(title)
                let escapedAuthor = self.escapeForJS(author)
                let escapedKeyword = self.escapeForJS(keyword)
                
                let isFav = QuoteStorage.shared.favorites.contains { $0.text.trimmingCharacters(in: .whitespacesAndNewlines) == text.trimmingCharacters(in: .whitespacesAndNewlines) }
                let favString = isFav ? "true" : "false"
                
                var jsEnd = "try { "
                jsEnd += "var display = document.getElementById('quote-text'); "
                jsEnd += "var sourceArea = document.getElementById('source-area'); "
                jsEnd += "if (display) { display.style.transition = ''; display.style.opacity = ''; } "
                jsEnd += "if (sourceArea) { sourceArea.style.transition = ''; sourceArea.style.opacity = ''; } "
                jsEnd += "if(window.setSearchKeyword) { window.setSearchKeyword('" + escapedKeyword + "'); } "
                jsEnd += "if(window.displayQuoteWithFade) { window.displayQuoteWithFade('" + escapedText + "', '" + escapedTitle + "', '" + escapedAuthor + "'); } "
                jsEnd += "setTimeout(() => { var icon = document.querySelector('#btn-star svg'); if(icon) { "
                jsEnd += "if (" + favString + ") { icon.classList.add('stocked'); icon.style.fill = '#ff9500'; icon.style.color = '#ff9500'; } "
                jsEnd += "else { icon.classList.remove('stocked'); icon.style.fill = 'none'; icon.style.color = ''; } "
                jsEnd += "} }, 50); "
                jsEnd += "} catch(e) { console.error(e); }"
                
                self.webView?.evaluateJavaScript(jsEnd, completionHandler: nil)
            }
        }

        func speechSynthesizer(_ synthesizer: AVSpeechSynthesizer, didStart utterance: AVSpeechUtterance) {
            if utterance.volume < 0.01 { return }
        }
        
        func speechSynthesizer(_ synthesizer: AVSpeechSynthesizer, didFinish utterance: AVSpeechUtterance) {
            if utterance.volume < 0.01 { return }
            
            if !textQueue.isEmpty {
                DispatchQueue.main.asyncAfter(deadline: .now() + 0.5) {
                    if !synthesizer.isSpeaking && self.isPlayingQuote {
                        self.playNextInQueue()
                    }
                }
            } else {
                DispatchQueue.main.asyncAfter(deadline: .now() + 0.1) {
                    if !synthesizer.isSpeaking {
                        self.isPlayingQuote = false
                        self.toggleAudioIcon(isPlaying: false)
                    }
                }
            }
        }
        
        func speechSynthesizer(_ synthesizer: AVSpeechSynthesizer, didCancel utterance: AVSpeechUtterance) {
            if utterance.volume < 0.01 { return }
        }
        
        func toggleAudioIcon(isPlaying: Bool) {
            let offSVG = "<svg viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"currentColor\" stroke-linecap=\"round\" stroke-linejoin=\"round\"><polygon points=\"11 5 6 9 2 9 2 15 6 15 11 19 11 5\"></polygon><path d=\"M19.07 4.93a10 10 0 0 1 0 14.14M15.54 8.46a5 5 0 0 1 0 7.07\"></path></svg>"
            let onSVG = "<svg viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"currentColor\" stroke-linecap=\"round\" stroke-linejoin=\"round\"><polygon points=\"11 5 6 9 2 9 2 15 6 15 11 19 11 5\"></polygon><path d=\"M15.54 8.46a5 5 0 0 1 0 7.07\"></path></svg>"
            
            let scale = isPlaying ? "1.2" : "1.0"
            let color = isPlaying ? "#ff9500" : ""
            let targetSVG = isPlaying ? onSVG : offSVG
            
            var js = "var btn = document.getElementById('btn-book');"
            js += " if(btn) {"
            js += " btn.innerHTML = '" + targetSVG + "';"
            js += " btn.style.transform = 'scale(" + scale + ")';"
            js += " btn.style.color = '" + color + "';"
            js += " }"
            
            DispatchQueue.main.async {
                self.webView?.evaluateJavaScript(js, completionHandler: nil)
            }
        }
    }
}
