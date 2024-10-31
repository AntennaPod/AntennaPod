package de.danoeh.antennapod.net.discovery;

import androidx.annotation.StringRes;
import de.danoeh.antennapod.net.common.UserAgentInterceptor;
import okhttp3.Request;

import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public abstract class PodcastIndexApi {
    public static Request.Builder buildAuthenticatedRequest(String url) {
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        calendar.clear();
        Date now = new Date();
        calendar.setTime(now);
        long secondsSinceEpoch = calendar.getTimeInMillis() / 1000L;
        String apiHeaderTime = String.valueOf(secondsSinceEpoch);
        String data4Hash = BuildConfig.PODCASTINDEX_API_KEY + BuildConfig.PODCASTINDEX_API_SECRET + apiHeaderTime;
        String hashString = sha1(data4Hash);

        return new Request.Builder()
                .addHeader("X-Auth-Date", apiHeaderTime)
                .addHeader("X-Auth-Key", BuildConfig.PODCASTINDEX_API_KEY)
                .addHeader("Authorization", hashString)
                .addHeader("User-Agent", UserAgentInterceptor.USER_AGENT)
                .url(url);
    }

    private static String sha1(String clearString) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-1");
            messageDigest.update(clearString.getBytes("UTF-8"));
            return toHex(messageDigest.digest());
        } catch (Exception ignored) {
            ignored.printStackTrace();
            return null;
        }
    }

    private static String toHex(byte[] bytes) {
        StringBuilder buffer = new StringBuilder();
        for (byte b : bytes) {
            buffer.append(String.format(Locale.getDefault(), "%02x", b));
        }
        return buffer.toString();
    }

    public static @StringRes int getCategoryName(int categoryId) {
        switch (categoryId) {
            case 1:
                return R.string.category_arts;
            case 2:
                return R.string.category_books;
            case 3:
                return R.string.category_design;
            case 4:
                return R.string.category_fashion;
            case 5:
                return R.string.category_beauty;
            case 6:
                return R.string.category_food;
            case 7:
                return R.string.category_performing;
            case 8:
                return R.string.category_visual;
            case 9:
                return R.string.category_business;
            case 10:
                return R.string.category_careers;
            case 11:
                return R.string.category_entrepreneurship;
            case 12:
                return R.string.category_investing;
            case 13:
                return R.string.category_management;
            case 14:
                return R.string.category_marketing;
            case 15:
                return R.string.category_non_profit;
            case 16:
                return R.string.category_comedy;
            case 17:
                return R.string.category_interviews;
            case 18:
                return R.string.category_improv;
            case 19:
                return R.string.category_stand_up;
            case 20:
                return R.string.category_education;
            case 21:
                return R.string.category_courses;
            case 22:
                return R.string.category_how_to;
            case 23:
                return R.string.category_language;
            case 24:
                return R.string.category_learning;
            case 25:
                return R.string.category_self_improvement;
            case 26:
                return R.string.category_fiction;
            case 27:
                return R.string.category_drama;
            case 28:
                return R.string.category_history;
            case 29:
                return R.string.category_health;
            case 30:
                return R.string.category_fitness;
            case 31:
                return R.string.category_alternative;
            case 32:
                return R.string.category_medicine;
            case 33:
                return R.string.category_mental;
            case 34:
                return R.string.category_nutrition;
            case 35:
                return R.string.category_sexuality;
            case 36:
                return R.string.category_kids;
            case 37:
                return R.string.category_family;
            case 38:
                return R.string.category_parenting;
            case 39:
                return R.string.category_pets;
            case 40:
                return R.string.category_animals;
            case 41:
                return R.string.category_stories;
            case 42:
                return R.string.category_leisure;
            case 43:
                return R.string.category_animation;
            case 44:
                return R.string.category_manga;
            case 45:
                return R.string.category_automotive;
            case 46:
                return R.string.category_aviation;
            case 47:
                return R.string.category_crafts;
            case 48:
                return R.string.category_games;
            case 49:
                return R.string.category_hobbies;
            case 50:
                return R.string.category_home;
            case 51:
                return R.string.category_garden;
            case 52:
                return R.string.category_video_games;
            case 53:
                return R.string.category_music;
            case 54:
                return R.string.category_commentary;
            case 55:
                return R.string.category_news;
            case 56:
                return R.string.category_daily;
            case 57:
                return R.string.category_entertainment;
            case 58:
                return R.string.category_government;
            case 59:
                return R.string.category_politics;
            case 60:
                return R.string.category_buddhism;
            case 61:
                return R.string.category_christianity;
            case 62:
                return R.string.category_hinduism;
            case 63:
                return R.string.category_islam;
            case 64:
                return R.string.category_judaism;
            case 65:
                return R.string.category_religion;
            case 66:
                return R.string.category_spirituality;
            case 67:
                return R.string.category_science;
            case 68:
                return R.string.category_astronomy;
            case 69:
                return R.string.category_chemistry;
            case 70:
                return R.string.category_earth;
            case 71:
                return R.string.category_life;
            case 72:
                return R.string.category_mathematics;
            case 73:
                return R.string.category_natural;
            case 74:
                return R.string.category_nature;
            case 75:
                return R.string.category_physics;
            case 76:
                return R.string.category_social;
            case 77:
                return R.string.category_society;
            case 78:
                return R.string.category_culture;
            case 79:
                return R.string.category_documentary;
            case 80:
                return R.string.category_personal;
            case 81:
                return R.string.category_journals;
            case 82:
                return R.string.category_philosophy;
            case 83:
                return R.string.category_places;
            case 84:
                return R.string.category_travel;
            case 85:
                return R.string.category_relationships;
            case 86:
                return R.string.category_sports;
            case 87:
                return R.string.category_baseball;
            case 88:
                return R.string.category_basketball;
            case 89:
                return R.string.category_cricket;
            case 90:
                return R.string.category_fantasy;
            case 91:
                return R.string.category_football;
            case 92:
                return R.string.category_golf;
            case 93:
                return R.string.category_hockey;
            case 94:
                return R.string.category_rugby;
            case 95:
                return R.string.category_running;
            case 96:
                return R.string.category_soccer;
            case 97:
                return R.string.category_swimming;
            case 98:
                return R.string.category_tennis;
            case 99:
                return R.string.category_volleyball;
            case 100:
                return R.string.category_wilderness;
            case 101:
                return R.string.category_wrestling;
            case 102:
                return R.string.category_technology;
            case 103:
                return R.string.category_true_crime;
            case 104:
                return R.string.category_tv;
            case 105:
                return R.string.category_film;
            case 106:
                return R.string.category_after_shows;
            case 107:
                return R.string.category_reviews;
            case 108:
                return R.string.category_climate;
            case 109:
                return R.string.category_weather;
            case 110:
                return R.string.category_tabletop;
            case 111:
                return R.string.category_role_playing;
            case 112:
                return R.string.category_cryptocurrency;
            default:
                throw new IllegalArgumentException("Unknown category");
        }
    }

    public static List<TopLevelCategory> getTopLevelCategories() {
        List<TopLevelCategory> result = new ArrayList<>();
        result.add(new TopLevelCategory(R.string.category_arts, new int[] {1, 2, 3, 4, 5, 6, 7, 8}));
        result.add(new TopLevelCategory(R.string.category_business, new int[] {9, 10, 11, 12, 13, 14, 15, 112}));
        result.add(new TopLevelCategory(R.string.category_comedy, new int[] {16, 17, 18, 19}));
        result.add(new TopLevelCategory(R.string.category_education, new int[] {20, 21, 22, 23, 24, 25, 28}));
        result.add(new TopLevelCategory(R.string.category_fiction, new int[] {26, 27}));
        result.add(new TopLevelCategory(R.string.category_health, new int[] {29, 30, 31, 32, 33, 34, 35}));
        result.add(new TopLevelCategory(R.string.category_family, new int[] {36, 37, 38, 39, 40, 41}));
        result.add(new TopLevelCategory(R.string.category_leisure, new int[] {42, 43, 44, 45, 46, 47, 48,
                                                                              49, 50, 51, 52, 110, 111}));
        result.add(new TopLevelCategory(R.string.category_music, new int[] {53, 54}));
        result.add(new TopLevelCategory(R.string.category_news, new int[] {55, 56, 57, 58, 59, 109}));
        result.add(new TopLevelCategory(R.string.category_religion, new int[] {60, 61, 62, 63, 64, 65, 66}));
        result.add(new TopLevelCategory(R.string.category_science, new int[] {67, 68, 69, 70, 71, 72,
                                                                              73, 74, 75, 76, 108}));
        result.add(new TopLevelCategory(R.string.category_culture, new int[] {77, 78, 79, 80, 81, 82, 83, 84, 85}));
        result.add(new TopLevelCategory(R.string.category_sports, new int[] {86, 87, 88, 89, 90, 91, 92, 93, 94,
                                                                             95, 96, 97, 98, 99, 100, 101}));
        result.add(new TopLevelCategory(R.string.category_technology, new int[] {}));
        result.add(new TopLevelCategory(R.string.category_true_crime, new int[] {}));
        result.add(new TopLevelCategory(R.string.category_tv, new int[] {104, 105, 106, 107}));
        return result;
    }

    public static class TopLevelCategory {
        public final @StringRes int name;
        public final int[] subCategories;

        public TopLevelCategory(int name, int[] subCategories) {
            this.name = name;
            this.subCategories = subCategories;
        }
    }
}
