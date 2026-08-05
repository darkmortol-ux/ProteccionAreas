# ProteccionesAreas

Plugin para Paper 1.21.11 de protección de zonas con sistema de raideo mediante TNT especial.

## Requisitos

- **Vault** + un plugin de economía (ej. EssentialsX) instalados en el servidor para poder comprar protecciones

---

## Cómo funciona

### 1. Comprar y crear una protección

```
/proteccion comprar 16    → $500, área de 16x16
/proteccion comprar 32    → $1500, área de 32x32
```

Esto entrega un **palo encantado** ("Palo de Protección") con la descripción del tamaño comprado.
Haz **click derecho sobre un bloque** en el punto donde quieres el centro de tu protección: el
plugin calcula automáticamente el cuadrado (16x16 o 32x32) alrededor de ese punto y lo registra.
El palo se consume al usarse. La protección cubre toda la columna vertical (de la roca base hasta
el límite de altura del mundo), no solo la superficie.

Si el área elegida se solapa con otra protección ya existente, no se puede crear ahí.

### 2. Dentro de tu protección

- Nadie más puede romper ni colocar bloques.
- Nadie más puede abrir cofres, barriles, hornos, mesas de crafteo, mesas de encantamiento,
  dispensadores, etc. — a menos que lo agregues con `/proteccion trust <jugador>`.
- `/proteccion untrust <jugador>` — le quita el acceso.
- `/proteccion tp` — te teletransporta a tu primera protección.
- `/proteccion info` — muestra info de la protección donde estás parado.
- `/proteccion listar` — lista todas tus protecciones.
- `/proteccion eliminar` — elimina la protección donde estás parado.

### 3. Sistema de vidas y RAID

Cada jugador empieza con **15 vidas** (configurable en `config.yml` → `starting-lives`). Sobre su
cabeza aparece un texto como **"[Vidas: 12]"** (usando equipos de scoreboard, la forma estándar
para mostrar texto junto al nombre en Minecraft).

Cada muerte resta 1 vida. Al llegar a 0 vidas (después de morir 15 veces), el jugador queda
**RAIDEADO**: su etiqueta cambia a **"[RAID] (x, z)"**, mostrando las coordenadas de la
**primera** protección que creó en su vida (se recuerda ese dato aunque la borre después). A
partir de ahí, **cualquier jugador puede abrir cofres y usar bloques dentro de TODAS las
protecciones de ese jugador**, sin necesidad de estar en la lista de confianza.

Ver el estado de cualquiera: `/vidas <jugador>` (requiere `proteccion.admin` para ver el de otros).

> ⚠️ Nota técnica: el texto sobre la cabeza se muestra como sufijo del nombre vía scoreboard.
> Minecraft no trunca esto a 16 caracteres en versiones modernas, pero textos muy largos pueden
> verse recortados según el cliente. Las coordenadas se muestran solo en X y Z (no la altura).

**Formas de conseguir vidas extra:**

- **Automática por tiempo jugado**: cada `lives-per-days-played.days` días jugados (acumulado
  real de tiempo conectado, no días del calendario del mundo) se otorga 1 vida extra sola. Por
  defecto son 500 días. Se revisa cada minuto para todos los jugadores conectados.
- **Comprar**: `/vidas comprar` — da 1 vida extra por `lives-purchase.price` (por defecto $50,000
  vía Vault). Se puede desactivar con `lives-purchase.enabled: false`.
- **Comando admin**: `/vidas dar <jugador> <cantidad>` (requiere `proteccion.admin`) — otorga (o
  quita, con un número negativo) vidas extra a cualquier jugador, en línea o no.

Estas vidas extra se suman permanentemente a las 15 vidas base y solo bajan al morir.

### 4. TNT especial ("TNT DE USO PESADO")

La TNT normal de Minecraft **sigue completamente bloqueada** dentro de cualquier protección (no
rompe nada, como cualquier otra explosión). Solo la TNT especial puede romper construcción dentro
de una zona protegida — y aun así, **nunca** destruye los bloques de utilidad listados en
`config.yml` → `protected-blocks` (cofres, hornos, mesas de crafteo/encantamiento, librerías,
yunques, etc. — la lista completa está ahí y es editable).

Fuera de cualquier protección, toda explosión (TNT normal, especial, creepers, etc.) se comporta
100% vanilla.

> ⚠️ **Opción avanzada — `tnt-destroys-everything`**: en `config.yml` puedes activar
> `tnt-destroys-everything: true` para que la TNT especial deje de respetar la lista de bloques
> protegidos y **destruya absolutamente todo** dentro de una protección, incluidos cofres, hornos,
> mesas de encantamiento y cualquier objeto guardado. **Está desactivada por defecto** y el plugin
> imprime una advertencia en la consola al arrancar si la activas, porque los jugadores raideados
> perderán sus objetos guardados sin ninguna protección. Actívala solo si entiendes bien esta
> consecuencia.

**Paso 1 — Polvo de Ignición Pesado** (mesa de crafteo, 9 pólvora normal):

```
[P][P][P]
[P][P][P]     →  1x Polvo de Ignición Pesado
[P][P][P]
```

**Paso 2 — TNT DE USO PESADO** (mesa de crafteo):

```
[H][S][H]
[B][T][B]     →  1x TNT DE USO PESADO
[H][M][H]
```
- `H` = Polvo de Ignición Pesado (paso 1)
- `S` = Hilo (String)
- `B` = Polvo de Blaze
- `T` = TNT normal de Minecraft
- `M` = Crema de Magma

La TNT especial se ve como una TNT normal pero con brillo de encantamiento y su propio nombre.
**Funciona exactamente como una TNT normal**: se puede colocar y prender con eslabón, activar por
redstone, disparar desde un dispensador, encenderse con flecha de fuego, y se propaga por cadena
si otra explosión la alcanza (incluyendo si otra TNT la empuja hacia la detonación).

---
## Comandos y permisos

| Comando | Descripción |
|---|---|
| `/proteccion comprar <16\|32>` | Comprar un palo de protección |
| `/proteccion tp` | Teletransportarte a tu protección |
| `/proteccion trust <jugador>` | Dar acceso a cofres/bloques |
| `/proteccion untrust <jugador>` | Quitar acceso |
| `/proteccion info` | Ver info de la protección actual |
| `/proteccion listar` | Listar tus protecciones |
| `/proteccion eliminar` | Eliminar la protección actual |
| `/vidas [jugador]` | Ver vidas restantes / estado RAID |
| `/vidas comprar` | Comprar 1 vida extra con Vault |
| `/vidas dar <jugador> <cantidad>` | (Admin) Dar o quitar vidas extra a cualquier jugador |

- `proteccion.usar` (default: todos) — usar el sistema normalmente
- `proteccion.admin` (default: OP) — construir/interactuar en cualquier protección, ver vidas de otros

---

## Configuración (`config.yml`)

- `sizes.small/large` — bloques y precio de cada tamaño
- `starting-lives` — vidas iniciales (por defecto 15)
- `lives-per-days-played.enabled/days` — vida extra automática por tiempo jugado (por defecto 500 días)
- `lives-purchase.enabled/price` — compra de vidas extra con Vault (por defecto $50,000)
- `tnt-destroys-everything` — ⚠️ si es `true`, la TNT especial ignora `protected-blocks` y destruye todo (desactivado por defecto)
- `protected-blocks` — lista de materiales indestructibles con la TNT especial (ignorada si `tnt-destroys-everything` es `true`)
- `lives-tag-format` / `raid-tag-format` — formato del texto sobre la cabeza
- Mensajes de aviso al morir y al activarse un RAID

---
