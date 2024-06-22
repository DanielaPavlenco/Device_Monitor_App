# Device Monitor App

## Descriere
Această aplicație ajută utilizatorii să monitorizeze și să gestioneze performanța dispozitivelor lor Android, oferind statistici detaliate despre utilizarea CPU, GPU, și memorie, managementul setărilor de afișare, gestionarea aplicațiilor, optimizarea bateriei și a performanței.

## Funcționalități
- **CPU și GPU Monitoring**: Monitorizează performanțele procesorului și unității de procesare grafică.
- **Battery Management**: Monitorizează starea bateriei și optimizează consumul de energie.
- **Performance Booster**: Oferă opțiuni pentru optimizarea performanței, cum ar fi accelerarea GPU și ajustarea calității randării.
- **Display Options**: Permite ajustarea setărilor ecranului, inclusiv luminozitatea, rata de refresh și rezoluția.
- **Memory Monitoring**: Oferă informații detaliate despre utilizarea memoriei și permite eliberarea memoriei ocupate inutil.
- **Storage Monitoring**: Monitorizează utilizarea stocării și oferă informații despre diferite categorii de stocare.
- **App Management**: Facilitează gestionarea aplicațiilor instalate, permițând dezinstalarea și dezactivarea acestora.
- **Network Usage Stats**: Monitorizează utilizarea rețelei și optimizează setările de rețea.
- **Debug Terminal**: Permite executarea comenzilor și automatizarea sarcinilor printr-un terminal integrat.

## Cerințe de sistem

- Android 6.0 (Marshmallow) sau mai recent
- Permisiuni de acces root pentru funcționalități avansate
- Magisk pentru gestionarea root-ului (recomandat pentru dispozitivele emulate)

## Instalare
### Clonarea depozitului

Pentru a clona depozitul de pe GitHub, deschide un terminal și rulează următoarele comenzi:

git clone https://github.com/DanielaPavlenco/Device_Monitor_App.git

cd DeviceMonitorApp

## Deschiderea proiectului în Android Studio

**1.** Deschide Android Studio.

**2.** Selectează "Open an existing Android Studio project".

**3.** Navighează la locația unde ai clonat depozitul și selectează folderul proiectului.

## Configurarea și rularea aplicației

**1.** Asigură-te că ai un dispozitiv fizic sau un emulator configurat în Android Studio.

**2.** Dacă utilizezi un emulator, configurează-l pentru a rula Android cu API Level 33.

**3.** Apasă pe butonul "Run" pentru a compila și rula aplicația pe dispozitivul ales.

## Obținerea accesului root cu Magisk
### Pe un dispozitiv real

**1.** Descarcă și instalează Magisk Manager din pagina oficială Magisk.

   https://github.com/topjohnwu/Magisk
   https://medium.com/@sarang6489/rooting-android-device-magisk-72e05793a1fb

**2.** Deblochează bootloader-ul dispozitivului tău. Acest proces variază în funcție de producător și model, așa că asigură-te că urmezi instrucțiunile specifice dispozitivului tău. De exemplu, pentru dispozitivele Samsung, poți găsi instrucțiuni detaliate pentru a instala image.boot pe pagina lor oficială. 

**3.** Descarcă fișierul Magisk.zip de pe pagina oficială și copiază-l pe dispozitivul tău.

**4.** Boot în modul recovery folosind combinația de taste specifică dispozitivului tău(de exemplu, Volum jos + Power).

**5.** Flash-uiește fișierul Magisk.zip folosind o recuperare personalizată (de exemplu, TWRP).
      https://support.mobiledit.com/portal/en/kb/articles/how-to-flash-twrp-on-samsung-devices
      
**6.** Selectează fișierul și confirmă flash-ul. Asigură-te că fișierul este compatibil cu modelul telefonului tău. Pentru a descărca fișierul potrivit, poți verifica pe site-ul oficial și sa selectezi modelul corect al dispozitivului tău.
      https://twrp.me/Devices/#google_vignette
      
**7.** După finalizare, selectează "Reboot System".

**8.** După ce dispozitivul a pornit, deschide Magisk manager și verifică daca root-ul este activ.

### Pe un emulator

**1.** Configurează un nou dispozitiv virtual (AVD) în Android Studio, asigurându-te că utilizezi API Level 33.

**2.** Închide emulatorul dacă este deschis.

**3.** Descarcă imaginea de boot pentru dispozitivul emulat și parchează-o folosind Magisk:

   - Rulează `adb pull /path/to/emulator_boot.img` pentru a copia imaginea de boot de pe emulator pe PC-ul tău.
   - Deschide Magisk Manager pe PC și înserează imaginea de boot.
   - Rulează `adb push /path/to/patched_boot.img /path/to/emulator` pentru a încărca imaginea de boot înserată în emulator.
   - Rulează `adb reboot bootloader` și `fastboot boot /path/to/patched_boot.img` pentru a porni emulatorul cu imaginea de boot.
     
**4.** Deschide Magisk Manager și asigură-te că root-ul este activ.

## Permisiuni Speciale

Pentru a permite aplicației să modifice setările securizate ale sistemului, trebuie să rulezi următoarea comandă ADB după ce faci Run pentru a compila :

**adb shell pm grant com.example.devicemonitorapp android.permission.WRITE_SECURE_SETTINGS**

Aceasta este o permisiune care permite aplicațiilor să modifice setările securizate ale sistemului, cum ar fi activarea sau dezactivarea funcțiilor prcum opțiunile pentru dezvoltatori, modificarea setărilor de afișare și ajustarea configurației rețelei. 

**Atenție !** : Această permisiune este foarte restricționată, deoarece poate afecta comportamentul general și securitatea dispozitivului.
