package io.github.ptimulka.miecz.helpers

import kotlin.collections.iterator

object BookNameNormalizer {
    // Based on Bible Tysiąclecia abbreviations and Polish names
    private val bookMap = mapOf(
        "Rdz" to listOf("rdz", "rodzaju", "księga rodzaju", "rodz", "gen"),
        "Wj" to listOf("wj", "wyjścia", "księga wyjścia", "wyj", "ex"),
        "Kpł" to listOf("kpł", "kapłańska", "księga kapłańska", "kapł", "lev", "kpl", "kapl"),
        "Lb" to listOf("lb", "liczb", "księga liczb", "num"),
        "Pwt" to listOf("pwt", "powtórzonego prawa", "księga powtórzonego prawa", "powtpr", "deut"),
        "Joz" to listOf("joz", "jozuego", "księga jozuego", "jos", "jozu"),
        "Sdz" to listOf("sdz", "sędziów", "księga sędziów", "sędz", "judg"),
        "Rt" to listOf("rt", "rut", "księga rut"),
        "1Sm" to listOf("1sm", "1 samuela", "pierwsza księga samuela", "1 samuel", "1 sm", "1sam", "1sa", "1 sam", "1 sa"),
        "2Sm" to listOf("2sm", "2 samuela", "druga księga samuela", "2 samuel", "2 sm", "2sam", "2sa", "2 sam", "2 sa"),
        "1Krl" to listOf("1krl", "1 królów", "pierwsza księga królewska", "1 królewska", "1 krl", "1ki", "1 ki"),
        "2Krl" to listOf("2krl", "2 królów", "druga księga królewska", "2 królewska", "2 krl", "2ki", "2 ki"),
        "1Krn" to listOf("1krn", "1 kronik", "pierwsza księga kronik", "1 krn", "1ch", "1 ch"),
        "2Krn" to listOf("2krn", "2 kronik", "druga księga kronik", "2 krn", "2ch", "2 ch"),
        "Ezd" to listOf("ezd", "ezdrasza", "księga ezdrasza", "ezdr", "ezr"),
        "Ne" to listOf("ne", "nehemiasza", "księga nehemiasza", "neh"),
        "Tb" to listOf("tb", "tobiasza", "księga tobiasza", "tob"),
        "Jdt" to listOf("jdt", "judyty", "księga judyty"),
        "Est" to listOf("est", "estery", "księga estery", "esth"),
        "1Mch" to listOf("1mch", "1 machabejska", "pierwsza księga machabejska", "1 mch"),
        "2Mch" to listOf("2mch", "2 machabejska", "druga księga machabejska", "2 mch"),
        "Hi" to listOf("hi", "hioba", "księga hioba", "job", "hiob"),
        "Ps" to listOf("ps", "psalmów", "księga psalmów", "psalm", "psl", "psł", "psalmy"),
        "Prz" to listOf("prz", "przysłów", "księga przysłów", "przyp", "prov", "przysłowia"),
        "Koh" to listOf("koh", "koheleta", "księga koheleta", "eklezjastesa", "kohe", "ek", "kohelet"),
        "Pnp" to listOf("pnp", "pieśń nad pieśniami", "pieśń"),
        "Mdr" to listOf("mdr", "mądrości", "księga mądrości", "mad", "wis"),
        "Syr" to listOf("syr", "syracha", "mądrość syracha", "eklezjastyk", "syracydesa"),
        "Iz" to listOf("iz", "izajasza", "księga izajasza", "iza", "isa", "izajasz"),
        "Jr" to listOf("jr", "jeremiasza", "księga jeremiasza", "jer", "jeremieasz"),
        "Lm" to listOf("lm", "lamentacje", "treny", "lamentacje jeremiasza", "lam", "la"),
        "Ba" to listOf("ba", "barucha", "księga barucha", "bar", "baruch"),
        "Ez" to listOf("ez", "ezechiela", "księga ezechiela", "ezech", "eze", "ezechiel"),
        "Dn" to listOf("dn", "daniela", "księga daniela", "dan", "daniel"),
        "Oz" to listOf("oz", "ozeasza", "księga ozeasza", "hos", "ozeasz"),
        "Jl" to listOf("jl", "joela", "księga joela", "joel"),
        "Am" to listOf("am", "amosa", "księga amosa", "amos"),
        "Ab" to listOf("ab", "abdiasza", "księga abdiasza", "abd", "ob", "abdiasz"),
        "Jon" to listOf("jon", "jonasza", "księga jonasza", "jona", "jonasz"),
        "Mi" to listOf("mi", "micheasza", "księga micheasza", "mich", "mic", "micheasz"),
        "Na" to listOf("na", "nahuma", "księga nahuma", "nah", "nahum"),
        "Ha" to listOf("ha", "habakuka", "księga habakuka", "habakuk"),
        "So" to listOf("so", "sofoniasza", "księga sofoniasza", "sofoniasz", "sof"),
        "Ag" to listOf("ag", "aggeusza", "księga aggeusza", "aggeusz", "agg", "hag"),
        "Za" to listOf("za", "zachariasza", "księga zachariasza", "zach", "zech", "zachariasz"),
        "Ml" to listOf("ml", "malachiasza", "księga malachiasza", "malachiasz", "mal"),
        "Mt" to listOf("mt", "mateusza", "ewangelia wg św. mateusza", "mat", "mateusz"),
        "Mk" to listOf("mk", "marka", "ewangelia wg św. marka", "mar", "marek"),
        "Łk" to listOf("łk", "łukasza", "ewangelia wg św. łukasza", "łukasz", "lk", "łuk", "luk", "lukasz"),
        "J" to listOf("j", "jana", "ewangelia wg św. jana", "jan"),
        "Dz" to listOf("dz", "dzieje", "dzieje apostolskie", "act"),
        "Rz" to listOf("rz", "rzymian", "list do rzymian", "rom"),
        "1Kor" to listOf("1kor", "1 koryntian", "pierwszy list do koryntian", "1 kor"),
        "2Kor" to listOf("2kor", "2 koryntian", "drugi list do koryntian", "2 kor"),
        "Ga" to listOf("ga", "galatów", "list do galatów", "gal"),
        "Ef" to listOf("ef", "efezjan", "list do efezjan", "efez", "eph"),
        "Flp" to listOf("flp", "filipian", "list do filipian", "php"),
        "Kol" to listOf("kol", "kolosan", "list do kolosan", "col"),
        "1Tes" to listOf("1tes", "1 tesaloniczan", "pierwszy list do tesaloniczan", "1 tes"),
        "2Tes" to listOf("2tes", "2 tesaloniczan", "drugi list do tesaloniczan", "2 tes"),
        "1Tm" to listOf("1tm", "1 tymoteusza", "pierwszy list do tymoteusza", "1 tym", "1 tm", "1tym"),
        "2Tm" to listOf("2tm", "2 tymoteusza", "drugi list do tymoteusza", "2 tym", "2 tm", "2tym"),
        "Tt" to listOf("tt", "tytusa", "list do tytusa", "tyt"),
        "Flm" to listOf("flm", "filemona", "list do filemona", "filem", "phlm"),
        "Hbr" to listOf("hbr", "hebrajczyków", "list do hebrajczyków", "heb"),
        "Jk" to listOf("jk", "jakuba", "list św. jakuba", "jak"),
        "1P" to listOf("1p", "1 piotra", "pierwszy list św. piotra", "1 p", "1pt", "1 pt"),
        "2P" to listOf("2p", "2 piotra", "drugi list św. piotra", "2 p", "2pt", "2 pt"),
        "1J" to listOf("1j", "1 jana", "pierwszy list św. jana", "1 j", "1jan", "1 jan", "1jn", "1 jn"),
        "2J" to listOf("2j", "2 jana", "drugi list św. jana", "2 j", "2jan", "2 jan", "2jn", "2 jn"),
        "3J" to listOf("3j", "3 jana", "trzeci list św. jana", "3 j", "3jan", "3 jan", "3jn", "3 jn"),
        "Jud" to listOf("jud", "judy", "list św. judy"),
        "Ap" to listOf("ap", "apokalipsa", "objawienie św. jana", "obj", "rev")
    )

    /**
     * Takes user input and returns the canonical Bible book sigla if a match is found.
     * Comparison is case-insensitive and supports various abbreviations and full names.
     */
    fun getCanonicalSigla(userInput: String): String? {
        val normalized = userInput.trim().lowercase()
        for ((canonical, names) in bookMap) {
            if (names.contains(normalized) || canonical.lowercase() == normalized) {
                return canonical
            }
        }
        return null
    }
}
