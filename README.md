# CentroTareas

App Android de práctica sobre **hilos y corrutinas**: hace tareas que tardan sin congelar la pantalla.

> Laboratorio N.° 14 — Hilos en Android · Programación en Móviles (TECSUP) · Farid Chávez Campos

## Observación del Paso 1

**¿Qué pasa con la pantalla?**
Al pulsar **Tarea larga (MAL)** la app se congela durante unos 4 segundos: el botón **Tócame** no suma, los botones no reaccionan y nada se redibuja. Cuando termina la espera, los toques que hice mientras estaba congelada se procesan de golpe y el texto pasa directamente de **Listo** a **Terminó**.

**¿Por qué no se ve "Trabajando..."?**
`tareaLargaMala()` se ejecuta en el **hilo principal**, que es el mismo que dibuja la pantalla y atiende los toques. El código cambia el mensaje a "Trabajando...", pero la pantalla no se redibuja hasta que el hilo principal queda libre. Como `Thread.sleep(4000)` lo bloquea 4 segundos y justo después el mensaje cambia a "Terminó", cuando Compose por fin puede dibujar el `StateFlow` ya tiene el último valor ("Terminó"). "Trabajando..." nunca llega a pintarse.

**¿Qué mostraría Android si la espera fuera de 10 segundos?**
Mostraría el diálogo **ANR** (*Application Not Responding*, "La app no responde"), con las opciones **Esperar** o **Cerrar app**. Android lo muestra cuando el hilo principal no responde a un evento del usuario (como un toque) en unos **5 segundos**. Con 10 segundos bloqueado, cualquier toque superaría ese límite.