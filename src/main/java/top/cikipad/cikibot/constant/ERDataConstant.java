package top.cikipad.cikibot.constant;

public class ERDataConstant {
    public static final String BASE_URL = "https://er.dakgg.io/api/v1/data";
    public static final String LANG_PARAM = "?hl=zh_CN";
    
    public static final String SEASONS_URL = BASE_URL + "/seasons" + LANG_PARAM;
    public static final String MASTERIES_URL = BASE_URL + "/masteries" + LANG_PARAM;
    public static final String MONSTERS_URL = BASE_URL + "/monsters" + LANG_PARAM;
    public static final String CHARACTERS_URL = BASE_URL + "/characters" + LANG_PARAM;
    public static final String ITEMS_URL = BASE_URL + "/items" + LANG_PARAM;
    public static final String SKILLS_URL = BASE_URL + "/skills" + LANG_PARAM;
    public static final String TACTICAL_SKILLS_URL = BASE_URL + "/tactical-skills" + LANG_PARAM;
    public static final String TRAIT_SKILLS_URL = BASE_URL + "/trait-skills" + LANG_PARAM;
    public static final String INFUSIONS_URL = BASE_URL + "/infusions" + LANG_PARAM;
    public static final String TIERS_URL = BASE_URL + "/tiers" + LANG_PARAM;
    public static final String AREAS_URL = BASE_URL + "/areas" + LANG_PARAM;
    public static final String WEATHERS_URL = BASE_URL + "/weathers" + LANG_PARAM;

    private ERDataConstant() {
        throw new IllegalStateException("Constant class");
    }
} 