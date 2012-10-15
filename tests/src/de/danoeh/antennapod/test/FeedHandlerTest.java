package de.danoeh.antennapod.test;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;

import android.test.AndroidTestCase;
import android.util.Log;
import de.danoeh.antennapod.feed.Feed;
import de.danoeh.antennapod.feed.FeedItem;
import de.danoeh.antennapod.syndication.handler.FeedHandler;

/** Enqueues a list of Feeds and tests if they are parsed correctly */
public class FeedHandlerTest extends AndroidTestCase {
	private static final String TAG = "FeedHandlerTest";
	private static final String FEEDS_DIR = "testfeeds";

	private static final String[] urls = {
			"http://bitlove.org/nsemak/mikrodilettanten/feed",
			"http://bitlove.org/moepmoeporg/riotburnz/feed",
			"http://bitlove.org/moepmoeporg/schachcast/feed",
			"http://bitlove.org/moepmoeporg/sundaymoaning/feed",
			"http://bitlove.org/motofunk/anekdotkast/feed",
			"http://bitlove.org/motofunk/motofunk/feed",
			"http://bitlove.org/nerdinand/zch/feed",
			"http://podcast.homerj.de/podcasts.xml",
			"http://www.dradio.de/rss/podcast/sendungen/wissenschaftundbildung/",
			"http://www.dradio.de/rss/podcast/sendungen/wirtschaftundverbraucher/",
			"http://www.dradio.de/rss/podcast/sendungen/literatur/",
			"http://www.dradio.de/rss/podcast/sendungen/sport/",
			"http://www.dradio.de/rss/podcast/sendungen/wirtschaftundgesellschaft/",
			"http://www.dradio.de/rss/podcast/sendungen/filmederwoche/",
			"http://www.blacksweetstories.com/feed/podcast/",
			"http://feeds.5by5.tv/buildanalyze",
			"http://bitlove.org/ranzzeit/ranz/feed",
			"http://bitlove.org/importthis/mp3/feed",
			"http://bitlove.org/astro/youtube/feed",
			"http://bitlove.org/channelcast/channelcast/feed",
			"http://bitlove.org/cccb/chaosradio/feed",
			"http://bitlove.org/astro/bitlove-show/feed",
			"http://feeds.thisamericanlife.org/talpodcast",
			"http://www.casasola.de/137b/1337motiv/1337motiv.xml",
			"http://alternativlos.org/ogg.rss", "http://www.bitsundso.de/feed",
			"http://www.gamesundso.de/feed/",
			"http://chaosradio.ccc.de/chaosradio-latest.rss",
			"http://feeds.feedburner.com/cre-podcast",
			"http://feeds.feedburner.com/NotSafeForWorkPodcast",
			"http://feeds.feedburner.com/mobile-macs-podcast",
			"http://www.gamesundso.de/feed/",
			"http://feeds.feedburner.com/DerLautsprecher",
			"http://feeds.feedburner.com/raumzeit-podcast",
			"http://feeds.feedburner.com/TheLunaticFringe",
			"http://feeds.feedburner.com/Kuechenradioorg",
			"http://feeds.feedburner.com/Medienradio_Podcast_RSS",
			"http://feeds.feedburner.com/wrint/wrint",
			"http://retrozirkel.de/episodes.mp3.rss",
			"http://trackback.fritz.de/?feed=podcast",
			"http://feeds.feedburner.com/linuxoutlaws-ogg",
			"http://www.mevio.com/feeds/noagenda.xml",
			"http://podcast.hr2.de/derTag/podcast.xml",
			"http://feeds.feedburner.com/thechangelog",
			"http://leoville.tv/podcasts/floss.xml",
			"http://www.radiotux.de/index.php?/feeds/index.rss2",
			"http://megamagis.ch/episodes.mp3.rss",
			"http://www.eurogamer.net/rss/eurogamer_podcast_itunes.rss",
			"http://bobsonbob.de/?feed=rss2",
			"http://www.blacksweetstories.com/feed/podcast/",
			"http://www.eurogamer.net/rss/eurogamer_podcast_itunes.rss",
			"http://www.wikigeeks.de/feed/mp3",
			"http://diehoppeshow.de/podcast/feed.xml",
			"http://feeds.feedburner.com/ThisIsMyNextPodcast?format=xml",
			"http://bitlove.org/343max/maerchenstunde/feed",
			"http://bitlove.org/343max/wmr-aac/feed",
			"http://bitlove.org/343max/wmr-mp3/feed",
			"http://bitlove.org/343max/wmr-oga/feed",
			"http://bitlove.org/adamc1999/noagenda/feed",
			"http://bitlove.org/alexbrueckel/normalzeit_podcast/feed",
			"http://bitlove.org/alexbrueckel/normalzeit_podcast_mp3/feed",
			"http://bitlove.org/alexbrueckel/tisch3-podcast/feed",
			"http://bitlove.org/alexolma/iphoneblog/feed",
			"http://bitlove.org/andydrop/nachtnerd/feed",
			"http://bitlove.org/apollo40/ps3newsroom/feed",
			"http://bitlove.org/beapirate/hauptstadtpiraten/feed",
			"http://bitlove.org/benni/besondereumstaende/feed",
			"http://bitlove.org/bennihahn/unicast/feed",
			"http://bitlove.org/berndbloggt/wirschweifenab/feed",
			"http://bitlove.org/bildungsangst/spoiler-alert-mp3/feed",
			"http://bitlove.org/bildungsangst/spoiler-alert-ogg/feed",
			"http://bitlove.org/bildungsangst/troja-alert-mp3/feed",
			"http://bitlove.org/bildungsangst/troja-alert-ogg/feed",
			"http://bitlove.org/binaergewitter/talk/feed",
			"http://bitlove.org/binaergewitter/talk-ogg/feed",
			"http://bitlove.org/bitgamers/bitgamerscast/feed",
			"http://bitlove.org/boingsworld/boingsworld/feed",
			"http://bitlove.org/boris/appsacker/feed",
			"http://bitlove.org/boris/bam/feed",
			"http://bitlove.org/bruhndsoweiter/anycast-aac/feed",
			"http://bitlove.org/bruhndsoweiter/anycast-mp3/feed",
			"http://bitlove.org/bruhndsoweiter/schnackennet-m4a/feed",
			"http://bitlove.org/bruhndsoweiter/schnackennet-mp3/feed",
			"http://bitlove.org/bruhndsoweiter/schnackennet-ogg/feed",
			"http://bitlove.org/byteweise/bytecast/feed",
			"http://bitlove.org/byteweise/byteweiseaac/feed",
			"http://bitlove.org/c3d2/news/feed",
			"http://bitlove.org/c3d2/pentacast/feed",
			"http://bitlove.org/c3d2/pentamedia/feed",
			"http://bitlove.org/c3d2/pentamusic/feed",
			"http://bitlove.org/c3d2/pentaradio/feed",
			"http://bitlove.org/campuscast_cc/campuscast_cc/feed",
			"http://bitlove.org/carlito/schnittmuster/feed",
			"http://bitlove.org/carlito/yaycomics/feed",
			"http://bitlove.org/cccb/chaosradio/feed",
			"http://bitlove.org/ccculm/chaosseminar-mp4-high/feed",
			"http://bitlove.org/ccculm/chaosseminar-theora/feed",
			"http://bitlove.org/channelcast/channelcast/feed",
			"http://bitlove.org/chgrasse/freequency/feed",
			"http://bitlove.org/chgrasse/kiezradio/feed",
			"http://bitlove.org/christiansteiner/secondunit/feed",
			"http://bitlove.org/cinext/cinext/feed",
			"http://bitlove.org/ckater/schoeneecken/feed",
			"http://bitlove.org/ckater/schoeneecken-mp3/feed",
			"http://bitlove.org/cllassnig/bytegefluester/feed",
			"http://bitlove.org/cllassnig/nerdtirol/feed",
			"http://bitlove.org/cocoaheads/austria/feed",
			"http://bitlove.org/compod/compod/feed",
			"http://bitlove.org/consolmedia/consolpodcast/feed",
			"http://bitlove.org/couchblog/computerfix/feed",
			"http://bitlove.org/culinaricast/podcast/feed",
			"http://bitlove.org/d3v/die-drei-vogonen/feed",
			"http://bitlove.org/danielbuechele/luftpost/feed",
			"http://bitlove.org/deimhart/mp3/feed",
			"http://bitlove.org/deimhart/ogg/feed",
			"http://bitlove.org/derbastard/podcast/feed",
			"http://bitlove.org/derpoppe/poppeandpeople/feed",
			"http://bitlove.org/derpoppe/stammtischphilosophen/feed",
			"http://bitlove.org/devradio/devradio-music-mp3/feed",
			"http://bitlove.org/devradio/devradio-music-ogg/feed",
			"http://bitlove.org/devradio/devradio-nomusic-mp3/feed",
			"http://bitlove.org/devradio/devradio-nomusic-ogg/feed",
			"http://bitlove.org/die-halde/die-halde/feed",
			"http://bitlove.org/dirtyminutesleft/m4a/feed",
			"http://bitlove.org/dirtyminutesleft/mp3/feed",
			"http://bitlove.org/dominik/knutsens/feed",
			"http://bitlove.org/dominik/schnittchen/feed",
			"http://bitlove.org/driveeo/podcast/feed",
			"http://bitlove.org/einfachben/freibeuterhafen/feed",
			"http://bitlove.org/eintr8podcast/aac/feed",
			"http://bitlove.org/eintr8podcast/eptv/feed",
			"http://bitlove.org/eintr8podcast/mp3/feed",
			"http://bitlove.org/eteubert/satoripress-m4a/feed",
			"http://bitlove.org/fabu/indie-fresse/feed",
			"http://bitlove.org/faldrian/bofh-mp3/feed",
			"http://bitlove.org/faldrian/bofh-oga/feed",
			"http://bitlove.org/faldrian/faldriansfeierabend/feed",
			"http://bitlove.org/filmtonpodcast/filmtonpodcast/feed",
			"http://bitlove.org/firmadorsch/fahrradio/feed",
			"http://bitlove.org/frequenz9/feed/feed",
			"http://bitlove.org/gamefusion/feeds/feed",
			"http://bitlove.org/gamesandmacs/podcast/feed",
			"http://bitlove.org/geekweek/techpodcast/feed",
			"http://bitlove.org/germanstudent/apfelnet/feed",
			"http://bitlove.org/germanstudent/bruellaffencouch-enhanced/feed",
			"http://bitlove.org/germanstudent/bruellaffencouch-mp3/feed",
			"http://bitlove.org/germanstudent/kauderwelschavantgarde/feed",
			"http://bitlove.org/germanstudent/podccast-enhanced/feed",
			"http://bitlove.org/germanstudent/podccast-mp3/feed",
			"http://bitlove.org/geschichtendose/love/feed",
			"http://bitlove.org/gfm/atzbach/feed",
			"http://bitlove.org/gfm/rumsendende/feed",
			"http://bitlove.org/grizze/vtlive/feed",
			"http://bitlove.org/hackerfunk/hf-mp3/feed",
			"http://bitlove.org/hackerfunk/hf-ogg/feed",
			"http://bitlove.org/hasencore/podcast/feed",
			"http://bitlove.org/hoaxmaster/hoaxilla/feed",
			"http://bitlove.org/hoaxmaster/psychotalk/feed",
			"http://bitlove.org/hoaxmaster/skeptoskop/feed",
			"http://bitlove.org/hoersuppe/vorcast/feed",
			"http://bitlove.org/holgi/wrint/feed",
			"http://bitlove.org/ich-bin-radio/fir/feed",
			"http://bitlove.org/ich-bin-radio/rsff/feed",
			"http://bitlove.org/incerio/podcast/feed",
			"http://bitlove.org/jagdfunk/jagdfunk/feed",
			"http://bitlove.org/janlelis/rubykraut/feed",
			"http://bitlove.org/jed/feed1/feed",
			"http://bitlove.org/jupiterbroadcasting/coderradio/feed",
			"http://bitlove.org/jupiterbroadcasting/fauxshowhd/feed",
			"http://bitlove.org/jupiterbroadcasting/fauxshowmobile/feed",
			"http://bitlove.org/jupiterbroadcasting/lashd/feed",
			"http://bitlove.org/jupiterbroadcasting/lasmobile/feed",
			"http://bitlove.org/jupiterbroadcasting/scibytehd/feed",
			"http://bitlove.org/jupiterbroadcasting/scibytemobile/feed",
			"http://bitlove.org/jupiterbroadcasting/techsnap60/feed",
			"http://bitlove.org/jupiterbroadcasting/techsnapmobile/feed",
			"http://bitlove.org/jupiterbroadcasting/unfilterhd/feed",
			"http://bitlove.org/jupiterbroadcasting/unfiltermobile/feed",
			"http://bitlove.org/kassettenkind/trollfunk/feed",
			"http://bitlove.org/klangkammermedia/abgekuppelt/feed",
			"http://bitlove.org/klangkammermedia/derbalkoncast/feed",
			"http://bitlove.org/klangkammermedia/wortundstille/feed",
			"http://bitlove.org/kurzpod/kurzpod/feed",
			"http://bitlove.org/kurzpod/kurzpodm4a/feed",
			"http://bitlove.org/kurzpod/ogg/feed",
			"http://bitlove.org/langpod/lp/feed",
			"http://bitlove.org/legrex/videli-noch/feed",
			"http://bitlove.org/linucast/screencast/feed",
			"http://bitlove.org/lisnewsnetcasts/listen/feed",
			"http://bitlove.org/logenzuschlag/cinecast/feed",
			"http://bitlove.org/maha/1337kultur/feed",
			"http://bitlove.org/maha/klabautercast/feed",
			"http://bitlove.org/map/fanboys/feed",
			"http://bitlove.org/map/fanboys-mp3/feed",
			"http://bitlove.org/map/retrozirkel/feed",
			"http://bitlove.org/map/retrozirkel-mp3/feed",
			"http://bitlove.org/mappleconfusers/nachts-mp3/feed",
			"http://bitlove.org/mappleconfusers/nachts-mp4/feed",
			"http://bitlove.org/mappleconfusers/nachts-ogg/feed",
			"http://bitlove.org/mappleconfusers/nachts-pdf/feed",
			"http://bitlove.org/markus/robotiklabor/feed",
			"http://bitlove.org/martinschmidt/freiklettern/feed",
			"http://bitlove.org/mespotine/mespotine_sessions/feed",
			"http://bitlove.org/meszner/aether/feed",
			"http://bitlove.org/meszner/kulturwissenschaften/feed",
			"http://bitlove.org/metaebene/cre/feed",
			"http://bitlove.org/metaebene/der-lautsprecher/feed",
			"http://bitlove.org/metaebene/kolophon/feed",
			"http://bitlove.org/metaebene/logbuch-netzpolitik/feed",
			"http://bitlove.org/metaebene/mobilemacs/feed",
			"http://bitlove.org/metaebene/newz-of-the-world/feed",
			"http://bitlove.org/metaebene/not-safe-for-work/feed",
			"http://bitlove.org/metaebene/raumzeit/feed",
			"http://bitlove.org/metaebene/raumzeit-mp3/feed",
			"http://bitlove.org/metagamer/metagamer/feed",
			"http://bitlove.org/mfromm/collaborativerockers/feed",
			"http://bitlove.org/mfromm/explorism/feed",
			"http://bitlove.org/mfromm/transientesichten/feed",
			"http://bitlove.org/mhpod/pofacs/feed",
			"http://bitlove.org/michaela_w/michaelaswelt/feed",
			"http://bitlove.org/michaelgreth/sharepointpdcast/feed",
			"http://bitlove.org/mintcast/podcast/feed",
			"http://bitlove.org/mitgezwitschert/brandung/feed",
			"http://bitlove.org/moepmoeporg/anonnewsde/feed",
			"http://bitlove.org/moepmoeporg/contentcast/feed",
			"http://bitlove.org/moepmoeporg/dieseminarren/feed",
			"http://bitlove.org/moepmoeporg/emcast/feed",
			"http://bitlove.org/moepmoeporg/fhainalex/feed",
			"http://bitlove.org/moepmoeporg/fruehstueck/feed",
			"http://bitlove.org/moepmoeporg/galanoir/feed",
			"http://bitlove.org/moepmoeporg/julespodcasts/feed",
			"http://bitlove.org/moepmoeporg/knorkpod/feed",
			"http://bitlove.org/moepmoeporg/lecast/feed",
			"http://bitlove.org/moepmoeporg/moepspezial/feed",
			"http://bitlove.org/moepmoeporg/podsprech/feed",
			"http://bitlove.org/moepmoeporg/pottcast/feed",
			"http://bitlove.org/moepmoeporg/riotburnz/feed",
			"http://bitlove.org/moepmoeporg/schachcast/feed",
			"http://bitlove.org/moepmoeporg/sundaymoaning/feed",
			"http://bitlove.org/motofunk/anekdotkast/feed",
			"http://bitlove.org/motofunk/motofunk/feed",
			"http://bitlove.org/nerdinand/zch/feed",
			"http://bitlove.org/netzpolitik/netzpolitik-podcast/feed",
			"http://bitlove.org/netzpolitik/netzpolitik-tv/feed",
			"http://bitlove.org/nischenkultur/soziopod/feed",
			"http://bitlove.org/nitramred/staatsbuergerkunde/feed",
			"http://bitlove.org/nitramred/staatsbuergerkunde-mp3/feed",
			"http://bitlove.org/nsemak/elementarfragen/feed",
			"http://bitlove.org/nsemak/elementarfragenmp/feed",
			"http://bitlove.org/nsemak/mikrodilettanten/feed",
			"http://bitlove.org/oaad/oaad/feed",
			"http://bitlove.org/ohrkrampfteam/ohr/feed",
			"http://bitlove.org/omegatau/all/feed",
			"http://bitlove.org/omegatau/english-only/feed",
			"http://bitlove.org/omegatau/german-only/feed",
			"http://bitlove.org/onatcer/oal/feed",
			"http://bitlove.org/panikcast/panikcast/feed",
			"http://bitlove.org/panxatony/aviewinblue/feed",
			"http://bitlove.org/panxatony/macnemotv/feed",
			"http://bitlove.org/pattex/megamagisch_m4a/feed",
			"http://bitlove.org/pattex/megamagisch_mp3/feed",
			"http://bitlove.org/pausengespraeche/pausengespraeche/feed",
			"http://bitlove.org/pck/ez-und-geschlecht/feed",
			"http://bitlove.org/pck/gender-kolleg-marburg/feed",
			"http://bitlove.org/pck/genderzentrum_mr/feed",
			"http://bitlove.org/pck/hh/feed",
			"http://bitlove.org/pck/hh_mp3/feed",
			"http://bitlove.org/pck/hh_ogg/feed",
			"http://bitlove.org/pck/intercast/feed",
			"http://bitlove.org/pck/peachnerdznohero/feed",
			"http://bitlove.org/pck/peachnerdznohero_mp3/feed",
			"http://bitlove.org/philip/einfach-schwul/feed",
			"http://bitlove.org/philip/faselcast2/feed",
			"http://bitlove.org/philipbanse/kuechenradio/feed",
			"http://bitlove.org/philipbanse/medienradio/feed",
			"http://bitlove.org/philipbanse/studienwahl_tv_audio/feed",
			"http://bitlove.org/philipbanse/studienwahl_tv_video/feed",
			"http://bitlove.org/podsafepilot/pmp/feed",
			"http://bitlove.org/podsafepilot/psid/feed",
			"http://bitlove.org/ponytimepodcast/ponytimepodcast/feed",
			"http://bitlove.org/pratfm/strunt/feed",
			"http://bitlove.org/pressrecord/podcast/feed",
			"http://bitlove.org/pztv/alle_formate/feed",
			"http://bitlove.org/qbi/datenkanal-mp3/feed",
			"http://bitlove.org/qbi/datenkanal-ogg/feed",
			"http://bitlove.org/quotidianitaet/quotidianitaet/feed",
			"http://bitlove.org/radiotux/radiotux-all/feed",
			"http://bitlove.org/randgruppenfunk/mediale_subkultur/feed",
			"http://bitlove.org/ranzzeit/ranz/feed",
			"http://bitlove.org/relet/lifeartificial_partone/feed",
			"http://bitlove.org/retinacast/pilotenpruefung/feed",
			"http://bitlove.org/retinacast/podcast/feed",
			"http://bitlove.org/retinacast/podcast-aac/feed",
			"http://bitlove.org/retinacast/retinauten/feed",
			"http://bitlove.org/retinacast/rtc/feed",
			"http://bitlove.org/revolutionarts/mehrspielerquote/feed",
			"http://bitlove.org/ronsens/machtdose/feed",
			"http://bitlove.org/rooby/fressefreiheit/feed",
			"http://bitlove.org/rundumpodcast/rundum/feed",
			"http://bitlove.org/ryuu/riesencast/feed",
			"http://bitlove.org/ryuu/ryuus_labercast/feed",
			"http://bitlove.org/sangyye/nerdmate/feed",
			"http://bitlove.org/sangyye/nerdmate-ogg/feed",
			"http://bitlove.org/schmalsprech/schmalsprech_m4a/feed",
			"http://bitlove.org/schmalsprech/schmalsprech_mp3/feed",
			"http://bitlove.org/schmidtlepp/houroflauer/feed",
			"http://bitlove.org/schmidtlepp/lauerinformiert/feed",
			"http://bitlove.org/sebastiansimon/wertungsfrei/feed",
			"http://bitlove.org/sebseb7/vimeo/feed",
			"http://bitlove.org/sirtomate/comichoehle/feed",
			"http://bitlove.org/smartphone7/windowsphonepodcast/feed",
			"http://bitlove.org/smcpodcast/feed/feed",
			"http://bitlove.org/sneakpod/cocktailpodcast/feed",
			"http://bitlove.org/sneakpod/sneakpod/feed",
			"http://bitlove.org/socialhack/hoerensagen/feed",
			"http://bitlove.org/socialhack/netzkinder/feed",
			"http://bitlove.org/socialhack/netzkinder_mp3/feed",
			"http://bitlove.org/socialhack/netzkinder_ogg/feed",
			"http://bitlove.org/socialhack/talking_anthropology/feed",
			"http://bitlove.org/sprechwaisen/sw/feed",
			"http://bitlove.org/sublab/aboutradio/feed",
			"http://bitlove.org/sysops/elektrisch/feed",
			"http://bitlove.org/sysops/hd/feed",
			"http://bitlove.org/taschencasts/taschencasts/feed",
			"http://bitlove.org/tcmanila/ae-podcast/feed",
			"http://bitlove.org/teezeit/kulturbuechse/feed",
			"http://bitlove.org/teezeit/kulturbuechse-mp3/feed",
			"http://bitlove.org/teezeit/teezeittalkradio/feed",
			"http://bitlove.org/teezeit/teezeittalkradio-mp3/feed",
			"http://bitlove.org/tinkengil/playtogether/feed",
			"http://bitlove.org/tobi_s/alleswirdgut/feed",
			"http://bitlove.org/toby/einschlafenenhanced/feed",
			"http://bitlove.org/toby/einschlafenpodcast/feed",
			"http://bitlove.org/toby/pubkameraden/feed",
			"http://bitlove.org/toby/pubkameradenaac/feed",
			"http://bitlove.org/tom/radioanstalt/feed",
			"http://bitlove.org/tvallgaeu/beitraege/feed",
			"http://bitlove.org/tvallgaeu/freizeit/feed",
			"http://bitlove.org/tvallgaeu/sendung/feed",
			"http://bitlove.org/ubahnverleih/teepodcast/feed",
			"http://bitlove.org/umunsherum/sammelcast/feed",
			"http://bitlove.org/umunsherum/spielonauten/feed",
			"http://bitlove.org/umunsherum/unteruns/feed",
			"http://bitlove.org/umunsherum/wasmachstdu/feed",
			"http://bitlove.org/uwe/nettesfrettchen/feed",
			"http://bitlove.org/vorgedacht/slug/feed",
			"http://bitlove.org/webdev/wdr/feed",
			"http://bitlove.org/weezerle/brandung/feed",
			"http://bitlove.org/weezerle/guestcast/feed",
			"http://bitlove.org/weezerle/stupalog/feed",
			"http://bitlove.org/wikigeeks/wikigeeks-aac/feed",
			"http://bitlove.org/wikigeeks/wikigeeks-mp3/feed",
			"http://bitlove.org/wikigeeks/wikigeeks-ogg/feed",
			"http://bitlove.org/workingdraft/revisionen/feed",
			"http://bitlove.org/wunderlich/podcast/feed",
			"http://www.cczwei.de/rss_tvissues.php" };

	private ArrayList<Feed> feeds;

	protected void setUp() throws Exception {
		super.setUp();
		feeds = new ArrayList<Feed>();
		for (int i = 0; i < urls.length; i++) {
			Feed f = new Feed(urls[i], new Date());
			f.setFile_url(new File(getContext().getExternalFilesDir(FEEDS_DIR)
					.getAbsolutePath(), "R" + i).getAbsolutePath());
			feeds.add(f);
		}
	}

	private InputStream getInputStream(String url)
			throws MalformedURLException, IOException {
		HttpURLConnection connection = (HttpURLConnection) (new URL(url))
				.openConnection();
		int rc = connection.getResponseCode();
		if (rc == HttpURLConnection.HTTP_OK) {
			return connection.getInputStream();
		} else {
			return null;
		}
	}

	private void downloadFeed(Feed feed) throws IOException {
		int num_retries = 20;
		boolean successful = false;

		for (int i = 0; i < num_retries; i++) {
			InputStream in = null;
			OutputStream out = null;
			try {
				in = getInputStream(feed.getDownload_url());
				assertNotNull(in);
				out = new BufferedOutputStream(new FileOutputStream(
						feed.getFile_url()));
				byte[] buffer = new byte[8 * 1024];
				int count = 0;
				while ((count = in.read(buffer)) != -1) {
					out.write(buffer, 0, count);
				}
				successful = true;
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				if (in != null) {
					in.close();
				}
				if (out != null) {
					out.close();
				}
				if (successful) {
					break;
				}
			}
		}
		if (!successful) {
			Log.e(TAG, "Download failed after " + num_retries + " retries");
			throw new IOException();
		}
	}

	private boolean isFeedValid(Feed feed) {
		Log.i(TAG, "Checking if " + feed.getDownload_url() + " is valid");
		boolean result = false;
		if (feed.getTitle() == null) {
			Log.e(TAG, "Feed has no title");
			return false;
		}
		if (feed.getItems() == null) {
			Log.e(TAG, "Feed has no items");
			return false;
		}
		if (!hasValidFeedItems(feed)) {
			Log.e(TAG, "Feed has invalid items");
			return false;
		}
		if (feed.getLink() == null) {
			Log.e(TAG, "Feed has no link");
			return false;
		}
		if (feed.getLink() != null && feed.getLink().length() == 0) {
			Log.e(TAG, "Feed has empty link");
			return false;
		}
		if (feed.getIdentifyingValue() == null) {
			Log.e(TAG, "Feed has no identifying value");
			return false;
		}
		if (feed.getIdentifyingValue() != null
				&& feed.getIdentifyingValue().length() == 0) {
			Log.e(TAG, "Feed has empty identifying value");
			return false;
		}
		return true;
	}

	private boolean hasValidFeedItems(Feed feed) {
		for (FeedItem item : feed.getItems()) {
			if (item.getTitle() == null) {
				Log.e(TAG, "Item has no title");
				return false;
			}
			if (item.getPubDate() == null) {
				Log.e(TAG, "Item has no pubDate");
				if (item.getTitle() != null) {
					Log.e(TAG, "Title of invalid item: " + item.getTitle());
				}
				return false;
			}
		}
		return true;
	}

	public void testParseFeeds() {
		Log.i(TAG, "Testing RSS feeds");
		while (!feeds.isEmpty()) {
			Feed feed = feeds.get(0);
			parseFeed(feed);
			feeds.remove(0);
		}

		Log.i(TAG, "RSS Test completed");
	}

	private void parseFeed(Feed feed) {
		try {
			Log.i(TAG, "Testing feed with url " + feed.getDownload_url());
			FeedHandler handler = new FeedHandler();
			downloadFeed(feed);
			handler.parseFeed(feed);
			assertTrue(isFeedValid(feed));
		} catch (Exception e) {
			Log.e(TAG, "Error when trying to test " + feed.getDownload_url());
			e.printStackTrace();
			fail();
		}
	}

	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
		for (Feed feed : feeds) {
			File f = new File(feed.getFile_url());
			f.delete();
		}
	}

}
