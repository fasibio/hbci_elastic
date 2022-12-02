package de.fasibio.hbciapp;

import java.io.File;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.Scanner;
import java.util.concurrent.ThreadLocalRandom;

import org.kapott.hbci.GV.HBCIJob;
import org.kapott.hbci.GV_Result.GVRKUms;
import org.kapott.hbci.GV_Result.GVRKUms.UmsLine;
import org.kapott.hbci.GV_Result.GVRSaldoReq;
import org.kapott.hbci.callback.AbstractHBCICallback;
import org.kapott.hbci.exceptions.HBCI_Exception;
import org.kapott.hbci.manager.BankInfo;
import org.kapott.hbci.manager.HBCIHandler;
import org.kapott.hbci.manager.HBCIUtils;
import org.kapott.hbci.manager.HBCIVersion;
import org.kapott.hbci.passport.AbstractHBCIPassport;
import org.kapott.hbci.passport.HBCIPassport;
import org.kapott.hbci.status.HBCIExecStatus;
import org.kapott.hbci.structures.Konto;
import org.kapott.hbci.structures.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

/**
 * Demo zum Abruf von Umsaetzen per PIN/TAN-Verfahren.
 * 
 * Die folgende Demo zeigt mit dem minimal noetigen Code, wie eine
 * Umsatz-Abfrage
 * fuer ein Konto durchgefuehrt werden kann. Hierzu wird der Einfachheit halber
 * das Verfahren PIN/TAN verwendet, da es von den meisten Banken unterstuetzt
 * wird.
 * 
 * Trage vor dem Ausfuehren des Programms die Zugangsdaten zu deinem Konto ein.
 */
public class UmsatzAbrufPinTan {

    /**
     * Die zu verwendende HBCI-Version.
     */
    private final static HBCIVersion VERSION = HBCIVersion.HBCI_300;

    String blz, user, pin;
    HBCIPassport passport;

    public UmsatzAbrufPinTan(String blz, String user, String pin) {
        this.blz = blz;
        this.user = user;
        this.pin = pin;

        // HBCI4Java initialisieren
        // In "props" koennen optional Kernel-Parameter abgelegt werden, die in der
        // Klasse
        // org.kapott.hbci.manager.HBCIUtils (oben im Javadoc) beschrieben sind.
        Properties props = new Properties();
        HBCIUtils.init(props, new MyHBCICallback(blz, user, pin));

        // In der Passport-Datei speichert HBCI4Java die Daten des Bankzugangs
        // (Bankparameterdaten, Benutzer-Parameter, etc.).
        // Die Datei kann problemlos geloescht werden. Sie wird beim naechsten mal
        // automatisch neu erzeugt,
        // wenn der Parameter "client.passport.PinTan.init" den Wert "1" hat (siehe
        // unten).
        // Wir speichern die Datei der Einfachheit halber im aktuellen Verzeichnis.
        final File passportFile = new File("testpassport.dat");

        // Wir setzen die Kernel-Parameter zur Laufzeit. Wir koennten sie alternativ
        // auch oben in "props" setzen.
        HBCIUtils.setParam("client.passport.default", "PinTan"); // Legt als Verfahren PIN/TAN fest.
        HBCIUtils.setParam("client.passport.PinTan.init", "1"); // Stellt sicher, dass der Passport initialisiert wird

        // Erzeugen des Passport-Objektes.
        HBCIPassport passport = AbstractHBCIPassport.getInstance(passportFile);

        // Konfigurieren des Passport-Objektes.
        // Das kann alternativ auch alles ueber den Callback unten geschehen

        // Das Land.
        passport.setCountry("DE");

        // Server-Adresse angeben. Koennen wir entweder manuell eintragen oder direkt
        // von HBCI4Java ermitteln lassen
        BankInfo info = HBCIUtils.getBankInfo(blz);
        passport.setHost(info.getPinTanAddress());

        // TCP-Port des Servers. Bei PIN/TAN immer 443, da das ja ueber HTTPS laeuft.
        passport.setPort(443);

        // Art der Nachrichten-Codierung. Bei Chipkarte/Schluesseldatei wird
        // "None" verwendet. Bei PIN/TAN kommt "Base64" zum Einsatz.
        passport.setFilterType("Base64");
        this.passport = passport;
    }

    public List<String> listKontos() {
        Konto[] konten = passport.getAccounts();
        List<String> result = new ArrayList<String>();
        for (Konto k : konten) {
            result.add(k.number);
        }
        return result;
    }

    public void collect(String kontoNumber, Boolean fakeSaldo) throws Exception {
        this.collectData(kontoNumber, null, fakeSaldo);
    }

    public void collect(String kontoNumber, Date startDate, Boolean fakeSaldo) throws Exception {
        this.collectData(kontoNumber, startDate, fakeSaldo);
    }

    private void collectData(String kontoNumber, Date startDate, Boolean fakeSaldo) throws Exception {

        // Das Handle ist die eigentliche HBCI-Verbindung zum Server
        HBCIHandler handle = null;

        try {
            // Verbindung zum Server aufbauen
            handle = new HBCIHandler(VERSION.getId(), passport);
            Logger log = Logger.getLogger();

            Konto k = passport.getAccount(kontoNumber);
            log = Logger.getLogger().addGlobalValues(
                    "konto_name", k.name,
                    "konto_type", k.type,
                    "konto_number", k.number,
                    "konto_acctype", k.acctype,
                    "konto_customerid", k.customerid,
                    "konto_blz", k.blz,
                    "konto_bic", k.bic);
            // 1. Auftrag fuer das Abrufen des Saldos erzeugen
            HBCIJob saldoJob = handle.newJob("SaldoReq");
            saldoJob.setParam("my", k); // festlegen, welches Konto abgefragt werden soll.
            saldoJob.addToQueue(); // Zur Liste der auszufuehrenden Auftraege hinzufuegen

            // 2. Auftrag fuer das Abrufen der Umsaetze erzeugen
            HBCIJob umsatzJob = handle.newJob("KUmsAll");
            umsatzJob.setParam("my", k); // festlegen, welches Konto abgefragt werden soll.
            if (startDate != null) {
                // Date startDate =
                // Date.from(LocalDate.now().minusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC));
                umsatzJob.setParam("startdate", startDate);
            }
            // umsatzJob.setParam("enddate",
            // Date.from(LocalDate.now().minusDays(1).atStartOfDay()));
            umsatzJob.addToQueue(); // Zur Liste der auszufuehrenden Auftraege hinzufuegen

            // Hier koennen jetzt noch weitere Auftraege fuer diesen Bankzugang hinzugefuegt
            // werden. Z.Bsp. Ueberweisungen.

            // Alle Auftraege aus der Liste ausfuehren.
            HBCIExecStatus status = handle.execute(); // @TODO hier vielleicht executeThreaded verwenden!!

            // Pruefen, ob die Kommunikation mit der Bank grundsaetzlich geklappt hat
            if (!status.isOK())
                error(status.toString());

            // Auswertung des Saldo-Abrufs.
            GVRSaldoReq saldoResult = (GVRSaldoReq) saldoJob.getJobResult();
            if (!saldoResult.isOK())
                error(saldoResult.toString());

            Value s = saldoResult.getEntries()[0].ready.value;

            log.info("Saldo", "saldo", fakeSaldo ? getNextFakeSaldo() : s.getLongValue(), "saldo_human",
                    fakeSaldo ? (getNextFakeSaldo() / 100) + "€" : s.toString());

            // Das Ergebnis des Jobs koennen wir auf "GVRKUms" casten. Jobs des Typs
            // "KUmsAll"
            // liefern immer diesen Typ.
            GVRKUms result = (GVRKUms) umsatzJob.getJobResult();

            // Pruefen, ob der Abruf der Umsaetze geklappt hat
            if (!result.isOK())
                error(result.toString());

            // Alle Umsatzbuchungen ausgeben
            List<UmsLine> buchungen = result.getFlatData();
            for (UmsLine buchung : buchungen) {
                List<String> zweck = buchung.usage;
                String zweckStr = "";
                if (zweck != null && zweck.size() > 0) {
                    for (String z : zweck) {
                        zweckStr += z;
                    }
                }

                // Ausgeben der Umsatz-Zeile
                log.info("Umsatzzeile",
                        "werstellung_date", buchung.valuta.toInstant().toString(),
                        "werstellung", fakeSaldo ? getNextFakeSaldo() : buchung.value.getLongValue(),
                        "werstellung_eur", fakeSaldo ? getNextFakeSaldo() / 100 : buchung.value.getLongValue() / 100,
                        "is_positive", buchung.value.getLongValue() > 0,
                        "werstellung_human", fakeSaldo ? (getNextFakeSaldo() / 100) + "€" : buchung.value.toString(),
                        "werstellung_useage", zweckStr,
                        "buchung_date", buchung.bdate.toInstant().toString(),
                        "buchung_id", buchung.id,
                        "saldo", fakeSaldo ? getNextFakeSaldo() : buchung.saldo.value.getLongValue(),
                        "saldo_human", fakeSaldo ? (getNextFakeSaldo() / 100) + "€" : buchung.saldo.value.toString());
            }
        } finally {
            // Sicherstellen, dass sowohl Passport als auch Handle nach Beendigung
            // geschlossen werden.
            if (handle != null)
                handle.close();

            if (passport != null)
                passport.close();
        }

    }

    private long getNextFakeSaldo() {
        return ThreadLocalRandom.current().nextInt(-5000, 5000) * 100;
    }

    /**
     * Ueber diesen Callback kommuniziert HBCI4Java mit dem Benutzer und fragt die
     * benoetigten
     * Informationen wie Benutzerkennung, PIN usw. ab.
     */
    private class MyHBCICallback extends AbstractHBCICallback {
        String blz;
        String user;
        String pin;

        public MyHBCICallback(String blz, String user, String pin) {
            this.blz = blz;
            this.user = user;
            this.pin = pin;
        }

        /**
         * @see org.kapott.hbci.callback.HBCICallback#log(java.lang.String, int,
         *      java.util.Date, java.lang.StackTraceElement)
         */
        @Override
        public void log(String msg, int level, Date date, StackTraceElement trace) {
            // Ausgabe von Log-Meldungen bei Bedarf
            // System.out.println(msg);
        }

        /**
         * @see org.kapott.hbci.callback.HBCICallback#callback(org.kapott.hbci.passport.HBCIPassport,
         *      int, java.lang.String, int, java.lang.StringBuffer)
         */
        @Override
        public void callback(HBCIPassport passport, int reason, String msg, int datatype, StringBuffer retData) {
            // Diese Funktion ist wichtig. Ueber die fragt HBCI4Java die benoetigten Daten
            // von uns ab.
            switch (reason) {
                // Mit dem Passwort verschluesselt HBCI4Java die Passport-Datei.
                // Wir nehmen hier der Einfachheit halber direkt die PIN. In der Praxis
                // sollte hier aber ein staerkeres Passwort genutzt werden.
                // Die Ergebnis-Daten muessen in dem StringBuffer "retData" platziert werden.
                case NEED_PASSPHRASE_LOAD:
                case NEED_PASSPHRASE_SAVE:
                    retData.replace(0, retData.length(), this.pin);
                    break;

                // PIN wird benoetigt
                case NEED_PT_PIN:
                    retData.replace(0, retData.length(), this.pin);
                    break;

                // BLZ wird benoetigt
                case NEED_BLZ:
                    retData.replace(0, retData.length(), this.blz);
                    break;

                // Die Benutzerkennung
                case NEED_USERID:
                    retData.replace(0, retData.length(), this.user);
                    break;

                // Die Kundenkennung. Meist identisch mit der Benutzerkennung.
                // Bei manchen Banken kann man die auch leer lassen
                case NEED_CUSTOMERID:
                    retData.replace(0, retData.length(), this.user);
                    break;

                ////////////////////////////////////////////////////////////////////////
                // Die folgenden Callbacks sind nur fuer die Ausfuehrung TAN-pflichtiger
                // Geschaeftsvorfaelle bei der Verwendung des PIN/TAN-Verfahrens noetig.
                // Z.Bsp. beim Versand einer Ueberweisung
                // "NEED_PT_SECMECH" kann jedoch auch bereits vorher auftreten.

                // HBCI4Java benoetigt die TAN per PhotoTAN-Verfahren
                // Liefert die anzuzeigende PhotoTAN-Grafik, die mit der entsprechenden
                // Smartphone-App der Bank fotografiert werden muss, um die TAN
                // zu generieren. Eine Implementierung muss diese Grafik anzeigen
                // sowie ein Eingabefeld fuer die TAN. Der Callback muss dann die vom
                // User eingegebene TAN zurueckliefern (nachdem dieser die Grafik
                // fotografiert und die App ihm die TAN angezeigt hat)
                case NEED_PT_PHOTOTAN:
                    // Die Klasse "MatrixCode" kann zum Parsen der Daten verwendet werden
                    try {
                        // MatrixCode code = new MatrixCode(retData.toString());

                        // Liefert den Mime-Type der grafik (i.d.R. "image/png").
                        // String type = code.getMimetype();

                        // Der Stream enthaelt jetzt die Binaer-Daten des Bildes
                        // InputStream stream = new ByteArrayInputStream(code.getImage());

                        // .... Hier Dialog mit der Grafik anzeigen und User-Eingabe der TAN
                        // Die Variable "msg" aus der Methoden-Signatur enthaelt uebrigens
                        // den bankspezifischen Text mit den Instruktionen fuer den User.
                        // Der Text aus "msg" sollte daher im Dialog dem User angezeigt
                        // werden.
                        String tan = null;
                        retData.replace(0, retData.length(), tan);
                    } catch (Exception e) {
                        throw new HBCI_Exception(e);
                    }

                    break;

                case NEED_PT_QRTAN:
                    // Die Klasse "QRCode" kann zum Parsen der Daten verwendet werden
                    try {
                        // QRCode code = new QRCode(retData.toString(),msg);

                        // Der Stream enthaelt jetzt die Binaer-Daten des Bildes
                        // InputStream stream = new ByteArrayInputStream(code.getImage());

                        // .... Hier Dialog mit der Grafik anzeigen und User-Eingabe der TAN
                        // Die Variable "msg" aus der Methoden-Signatur enthaelt uebrigens
                        // den bankspezifischen Text mit den Instruktionen fuer den User.
                        // Der Text aus "msg" sollte daher im Dialog dem User angezeigt
                        // werden. Da Sparkassen den eigentlichen Bild u.U. auch in msg verpacken,
                        // sollte zur Anzeige nicht der originale Text verwendet werden sondern
                        // der von QRCode - dort ist dann die ggf. enthaltene Base64-codierte QR-Grafik
                        // entfernt
                        // msg = code.getMessage();
                        String tan = null;
                        retData.replace(0, retData.length(), tan);
                    } catch (Exception e) {
                        throw new HBCI_Exception(e);
                    }

                    break;

                // HBCI4Java benoetigt den Code des verwendenden TAN-Verfahren (smsTAN,
                // chipTAN optisch, photoTAN,...)
                // I.d.R. ist das eine dreistellige mit "9" beginnende Ziffer
                case NEED_PT_SECMECH:

                    // Als Parameter werden die verfuegbaren TAN-Verfahren uebergeben.
                    // Der Aufbau des String ist wie folgt:
                    // <code1>:<name1>|<code2>:<name2>|...
                    // Bsp:
                    // 911:smsTAN|920:chipTAN optisch|955:photoTAN
                    // String options = retData.toString();

                    // Der Callback muss den Code des zu verwendenden TAN-Verfahrens
                    // zurueckliefern
                    // In "code" muss der 3-stellige Code des vom User gemaess obigen
                    // Optionen ausgewaehlte Verfahren eingetragen werden
                    String code = "";
                    retData.replace(0, retData.length(), code);
                    break;

                // HBCI4Java benoetigt die TAN per smsTAN/chipTAN/weiteren TAN-Verfahren
                case NEED_PT_TAN:

                    // Wenn per "retData" Daten uebergeben wurden, dann enthalten diese
                    // den fuer chipTAN optisch zu verwendenden Flickercode.
                    // Falls nicht, ist es eine TAN-Abfrage, fuer die keine weiteren
                    // Parameter benoetigt werden (z.Bsp. beim smsTAN-Verfahren)

                    // Die Variable "msg" aus der Methoden-Signatur enthaelt uebrigens
                    // den bankspezifischen Text mit den Instruktionen fuer den User.
                    // Der Text aus "msg" sollte daher im Dialog dem User angezeigt
                    // werden.

                    String flicker = retData.toString();
                    if (flicker != null && flicker.length() > 0) {
                        // Ist chipTAN optisch. Es muss ein animierter Barcode angezeigt
                        // werden. Hierfuer kann die Hilfsklasse "FlickerRenderer" verwendet
                        // werden. Diese enthalt bereits das Parsen. Es muss lediglich die
                        // Methode "paint" ueberschrieben werden.
                        // FlickerRenderer renderer = new FlickerRenderer(flicker);

                        // Hier TAN-Abfrage mit dem animierten Barcode anzeigen sowie
                        // Eingabefeld fuer die TAN
                        String tan = null;
                        retData.replace(0, retData.length(), tan);
                    } else {
                        // Ist smsTAN, iTAN, o.ae.
                        // Dialog zur TAN-Eingabe anzeigen mit dem Text aus "msg".
                        Scanner sc = new Scanner(System.in);
                        System.out.print("Enter tan: ");
                        String str = sc.nextLine();
                        sc.close();
                        String tan = str;
                        retData.replace(0, retData.length(), tan);
                    }

                    break;

                // Beim Verfahren smsTAN ist es moeglich, mehrere Handynummern mit
                // Aliasnamen bei der Bank zu hinterlegen. Auch wenn nur eine Handy-
                // Nummer bei der Bank hinterlegt ist, kann es durchaus passieren,
                // dass die Bank dennoch die Aufforderung zur Auswahl des TAN-Mediums
                // sendet.
                case NEED_PT_TANMEDIA:

                    // Als Parameter werden die verfuegbaren TAN-Medien uebergeben.
                    // Der Aufbau des String ist wie folgt:
                    // <name1>|<name2>|...
                    // Bsp:
                    // Privathandy|Firmenhandy
                    // String options = retData.toString();

                    // Der Callback muss den vom User ausgewaehlten Aliasnamen
                    // zurueckliefern. Falls "options" kein "|" enthaelt, ist davon
                    // auszugehen, dass nur eine moegliche Option existiert. In dem
                    // Fall ist keine Auswahl noetig und "retData" kann unveraendert
                    // bleiben
                    String alias = null;
                    retData.replace(0, retData.length(), alias);

                    break;
                //
                ////////////////////////////////////////////////////////////////////////

                // Manche Fehlermeldungen werden hier ausgegeben
                case HAVE_ERROR:
                    Logger.getLogger().info(msg);
                    break;

                default:
                    // Wir brauchen nicht alle der Callbacks
                    break;

            }
        }

        /**
         * @see org.kapott.hbci.callback.HBCICallback#status(org.kapott.hbci.passport.HBCIPassport,
         *      int, java.lang.Object[])
         */
        @Override
        public void status(HBCIPassport passport, int statusTag, Object[] o) {
            // So aehnlich wie log(String,int,Date,StackTraceElement) jedoch fuer
            // Status-Meldungen.
        }

    }

    /**
     * Beendet das Programm mit der angegebenen Fehler-Meldung.
     * 
     * @param msg die Meldung.
     */
    private static void error(String msg) {
        System.err.println(msg);
    }

}