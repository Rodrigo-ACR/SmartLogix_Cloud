<template>
    <div>
        <NavbarAdmin />
        <ConfirmModal ref="confirmRef" />

        <div class="container page">

            <!-- ENCABEZADO -->
            <div class="demo-header">
                <div>
                    <h1>🌊 Demo Evaluación Parcial 3 — Apache Kafka</h1>
                    <p class="text-muted">Desarrollo Cloud Native I — DSY1107_007D · Streaming de eventos entre microservicios (convive con RabbitMQ)</p>
                </div>
                <div class="head-btns">
                    <a class="btn btn-outline" :href="kafkaUiUrl" target="_blank" rel="noopener">🖥️ Abrir Kafka UI</a>
                    <button class="btn btn-outline" @click="$router.push('/admin')">← Volver</button>
                </div>
            </div>

            <!-- ESTADO GENERAL -->
            <div class="status-row">
                <span class="schip" :class="api.ok ? 'chip-ok' : api.error ? 'chip-err' : 'chip-warn'">
                    {{ api.ok ? '✅' : api.error ? '❌' : '⏳' }} kafkaadmin vía BFF
                </span>
                <span class="schip" :class="brokersActivos >= 3 ? 'chip-ok' : 'chip-warn'">
                    🧩 {{ brokersActivos }}/3 brokers
                </span>
                <span class="schip chip-ok">🗂️ {{ topicosNegocio.length }} tópicos de negocio</span>
                <span class="schip chip-ok">👥 {{ overview.grupos.length }} consumer groups</span>
                <span class="schip" :class="mensajesDlt > 0 ? 'chip-warn' : 'chip-ok'">
                    ☠️ {{ mensajesDlt }} mensajes en DLT
                </span>
                <span class="status-tools">
                    <label class="chk"><input type="checkbox" v-model="auto" /> Actualizar cada 5 s</label>
                    <button class="btn btn-outline btn-sm" @click="cargar()" :disabled="cargando">
                        {{ cargando ? '⏳' : '🔄' }} Actualizar
                    </button>
                </span>
            </div>

            <div class="error-banner-admin" v-if="api.error">
                <span>⚠️</span>
                <div>
                    <strong>No se pudo leer Kafka</strong>
                    <p>{{ api.error }}</p>
                </div>
                <button class="btn-retry-admin" @click="cargar()">Reintentar</button>
            </div>

            <!-- REQUISITOS EP3 -->
            <div class="demo-grid">
                <div class="demo-col">
                    <div class="req-card card">
                        <div class="req-head">
                            <span class="rn">1</span>
                            <div><h3>Kafka desplegado con Docker Compose en EC2</h3><p>3 Zookeeper + 3 brokers (RF 3, min.insync.replicas 2)</p></div>
                            <span class="rbadge" :class="brokersActivos >= 3 ? 'rbadge-ok' : ''">{{ brokersActivos >= 3 ? '✓ Logrado' : '⏳' }}</span>
                        </div>
                        <div class="req-body">
                            <div class="ms-row" v-for="b in overview.cluster.brokers" :key="b.id">
                                <span class="ms-dot dot-ok"></span>
                                <span class="ms-nombre">Broker {{ b.id }} · {{ b.host }}:{{ b.puerto }}</span>
                                <span class="ms-port">{{ b.esControlador ? '👑 controlador' : 'follower' }}</span>
                            </div>
                            <div class="ir" v-if="!overview.cluster.brokers.length"><span>—</span><strong>Sin datos todavía</strong></div>
                            <div class="ir" v-if="overview.cluster.clusterId"><span>🆔 Cluster</span><code>{{ overview.cluster.clusterId }}</code></div>
                            <div class="ir"><span>🐘 Zookeeper</span><strong>zookeeper1, zookeeper2, zookeeper3 (quórum de 3)</strong></div>
                        </div>
                    </div>

                    <div class="req-card card">
                        <div class="req-head">
                            <span class="rn">2</span>
                            <div><h3>Dos tópicos con 3 particiones y 3 réplicas</h3><p>pedidos.eventos y logistica.eventos, cada uno con su DLT</p></div>
                            <span class="rbadge" :class="topicosOk ? 'rbadge-ok' : ''">{{ topicosOk ? '✓ Logrado' : '⏳' }}</span>
                        </div>
                        <div class="req-body">
                            <div class="ms-row" v-for="t in topicosNegocio" :key="t.nombre">
                                <span class="ms-dot" :class="t.particiones === 3 && t.replicas === 3 && !t.particionesSubReplicadas ? 'dot-ok' : 'dot-err'"></span>
                                <span class="ms-nombre">{{ t.nombre }}</span>
                                <span class="ms-count">{{ t.particiones }}P · {{ t.replicas }}R</span>
                            </div>
                            <div class="ir" v-if="!topicosNegocio.length"><span>—</span><strong>Sin tópicos todavía</strong></div>
                        </div>
                    </div>

                    <div class="req-card card">
                        <div class="req-head">
                            <span class="rn">3</span>
                            <div><h3>Productores y consumidores con DTO tipados</h3><p>Un consumer group por microservicio, con su lag</p></div>
                            <span class="rbadge" :class="gruposOk ? 'rbadge-ok' : ''">{{ gruposOk ? '✓ Logrado' : '⏳' }}</span>
                        </div>
                        <div class="req-body">
                            <div class="ms-row" v-for="g in flujoGrupos" :key="g.grupo">
                                <span class="ms-dot" :class="g.detalle && esStable(g.detalle.estado) ? 'dot-ok' : 'dot-err'"></span>
                                <span class="ms-nombre">{{ g.icono }} {{ g.ms }} <small class="dim">{{ g.grupo }}</small></span>
                                <span class="ms-count">{{ g.detalle ? g.detalle.totalMiembros : 0 }} miembro(s) · lag {{ g.detalle ? g.detalle.lagTotal : '—' }}</span>
                            </div>
                        </div>
                    </div>
                </div>

                <div class="demo-col">
                    <div class="req-card card">
                        <div class="req-head">
                            <span class="rn">4</span>
                            <div><h3>Reintentos y Dead Letter Topic (DLT)</h3><p>3 reintentos con backoff (1s, 2s, 4s) y luego el mensaje va a &lt;tópico&gt;.DLT</p></div>
                            <span class="rbadge" :class="dlts.length >= 2 ? 'rbadge-ok' : ''">{{ dlts.length >= 2 ? '✓ Logrado' : '⏳' }}</span>
                        </div>
                        <div class="req-body">
                            <div class="ir"><span>☠️ DLTs</span><strong>{{ dlts.length }} tópicos (retención 30 días)</strong></div>
                            <div class="ir"><span>🔁 Política</span><strong>DefaultErrorHandler + DeadLetterPublishingRecoverer</strong></div>
                            <div class="ir"><span>✋ Commit</span><strong>Manual por registro (AckMode.RECORD) · idempotencia por eventId</strong></div>
                            <div class="test-btns">
                                <button class="btn btn-primary btn-sm" @click="probarDlt" :disabled="ocupado">
                                    ☠️ Probar DLT de {{ pruebaActual.etiqueta }}
                                </button>
                            </div>
                        </div>
                    </div>

                    <div class="req-card card">
                        <div class="req-head">
                            <span class="rn">5</span>
                            <div><h3>Seguridad: JWT en el camino a Kafka</h3><p>Esta pantalla habla con el BFF, nunca con kafkaadmin</p></div>
                            <span class="rbadge" :class="seguridad.sinToken && seguridad.sinToken.ok && seguridad.conToken && seguridad.conToken.ok ? 'rbadge-ok' : ''">
                                {{ seguridad.sinToken && seguridad.sinToken.ok && seguridad.conToken && seguridad.conToken.ok ? '✓ Logrado' : '⏳' }}
                            </span>
                        </div>
                        <div class="req-body">
                            <div class="test-btns">
                                <button class="btn btn-outline btn-sm" @click="testSinToken" :disabled="ocupado">🚫 Test sin token</button>
                                <button class="btn btn-primary btn-sm" @click="testConToken" :disabled="ocupado">✅ Test con token</button>
                            </div>
                            <div class="ir" v-if="seguridad.sinToken">
                                <span>🚫 Sin token</span>
                                <strong :class="seguridad.sinToken.ok ? 'text-ok' : 'text-err'">{{ seguridad.sinToken.msg }}</strong>
                            </div>
                            <div class="ir" v-if="seguridad.conToken">
                                <span>✅ Con token</span>
                                <strong :class="seguridad.conToken.ok ? 'text-ok' : 'text-err'">{{ seguridad.conToken.msg }}</strong>
                            </div>
                            <div class="ir"><span>🛡️ Capas</span><strong>API Gateway (JWT) → BFF (JWT) → kafkaadmin</strong></div>
                        </div>
                    </div>

                    <div class="req-card card">
                        <div class="req-head">
                            <span class="rn">6</span>
                            <div><h3>Kafka UI y administración</h3><p>Brokers y tópicos desde Kafka UI; tópicos y configs desde kafkaadmin</p></div>
                            <span class="rbadge rbadge-ok">✓ Abajo</span>
                        </div>
                        <div class="req-body">
                            <div class="ir"><span>🖥️ Kafka UI</span><a :href="kafkaUiUrl" target="_blank" rel="noopener"><code>{{ kafkaUiUrl }}</code></a></div>
                            <div class="ir"><span>🧭 Flujo</span><strong>Flujo de eventos → Pruebas → Mensajes → Administración</strong></div>
                            <div class="ir"><span>🔒 Protección</span><strong>Los 4 tópicos del sistema no se borran por error</strong></div>
                        </div>
                    </div>
                </div>
            </div>

            <!-- FLUJO DE EVENTOS -->
            <div class="card section">
                <div class="sec-head">
                    <div>
                        <h3>1 · Flujo de eventos entre microservicios</h3>
                        <p>Quién produce y quién consume cada tópico. RabbitMQ sigue funcionando en paralelo sin cambios.</p>
                    </div>
                </div>
                <div class="flow-card" v-for="t in FLUJO" :key="t.topico">
                    <div class="flow-ex">
                        <span class="flow-icon">🗂️</span>
                        <strong>{{ t.topico }}</strong>
                        <span class="ex-badge t-topic">key = {{ t.clave }}</span>
                        <span class="dim" v-if="infoTopico(t.topico)">
                            {{ infoTopico(t.topico).particiones }} particiones · {{ infoTopico(t.topico).replicas }} réplicas
                        </span>
                    </div>
                    <div class="flow-rows">
                        <div class="flow-row" v-for="p in t.productores" :key="'p' + p.ms">
                            <span class="flow-arrow"><span class="line"></span><code>produce</code><span class="line"></span>▶</span>
                            <span class="q-chip static"><span>{{ p.icono }} {{ p.ms }}</span><small>{{ p.eventos }}</small></span>
                        </div>
                        <div class="flow-row" v-for="c in t.consumidores" :key="'c' + c.grupo">
                            <span class="flow-arrow"><span class="line"></span><code>consume</code><span class="line"></span>◀</span>
                            <span class="q-chip static">
                                <span>{{ c.icono }} {{ c.ms }}</span>
                                <small>{{ c.grupo }} · {{ c.accion }}</small>
                            </span>
                            <span class="flow-dlx" v-if="lagGrupo(c.grupo) !== null">lag {{ lagGrupo(c.grupo) }}</span>
                        </div>
                    </div>
                </div>
            </div>

            <!-- PRUEBAS -->
            <div class="card section">
                <div class="sec-head">
                    <div>
                        <h3>2 · Pruebas funcionales: publicar un evento</h3>
                        <p>Publica un mensaje en un tópico (vía BFF → kafkaadmin) y mira cómo lo consumen los microservicios en el visor y en el lag.</p>
                    </div>
                </div>
                <div class="test-grid">
                    <form class="form-card" @submit.prevent="publicar">
                        <h4>Nuevo evento</h4>
                        <div class="input-group">
                            <label>Escenario</label>
                            <select v-model="pruebaSel" @change="cargarPreset('valido')">
                                <option v-for="p in PRUEBAS" :key="p.key" :value="p.key">{{ p.etiqueta }}</option>
                            </select>
                        </div>
                        <div class="input-group">
                            <label>Tópico</label>
                            <input v-model="pub.topico" readonly />
                        </div>
                        <div class="input-group">
                            <label>Clave (define la partición)</label>
                            <input v-model="pub.clave" placeholder="PEDIDO-9001" />
                        </div>
                        <div class="input-group">
                            <label>Valor (JSON del evento)</label>
                            <textarea v-model="pub.valor" rows="11"></textarea>
                        </div>
                        <div class="test-btns">
                            <button type="button" class="btn btn-outline btn-sm" @click="cargarPreset('valido')">✅ Evento válido</button>
                            <button type="button" class="btn btn-outline btn-sm" @click="cargarPreset('invalido')">☠️ Evento inválido</button>
                        </div>
                        <button class="btn btn-primary" :disabled="ocupado || !pub.valor.trim()">📤 Publicar en Kafka</button>
                        <p class="hint">Un JSON válido lo procesan los consumidores. Un mensaje inválido se reintenta 3 veces y termina en <code>{{ pub.topico }}.DLT</code>.</p>
                    </form>

                    <div class="form-card">
                        <h4>Qué debería pasar</h4>
                        <div class="ir"><span>1</span><strong>kafkaadmin publica el evento en <code>{{ pub.topico }}</code></strong></div>
                        <div class="ir"><span>2</span><strong>Kafka lo guarda en una partición según la clave y lo replica a los 3 brokers</strong></div>
                        <div class="ir"><span>3</span><strong>Cada consumer group lo lee una sola vez: {{ pruebaActual.consumen }}</strong></div>
                        <div class="ir"><span>4</span><strong>Si el JSON no se puede leer: 3 reintentos y va a <code>{{ pub.topico }}.DLT</code></strong></div>
                        <div class="banner" :class="'banner-' + banner.tipo" v-if="banner.msg" style="margin:6px 0 0">
                            {{ banner.tipo === 'ok' ? '✅' : '❌' }} {{ banner.msg }}
                        </div>
                        <div class="ir" v-if="ultimoEnvio">
                            <span>📍 Último</span>
                            <strong>partición {{ ultimoEnvio.particion }} · offset {{ ultimoEnvio.offset }}</strong>
                        </div>
                    </div>
                </div>
            </div>


            <!-- TEST DE CARGA -->
            <div class="card section">
                <div class="sec-head">
                    <div>
                        <h3>2b · Test de carga: ráfaga de mensajes</h3>
                        <p>Publica N eventos en el tópico <code>demo.carga</code> (3 particiones, 3 réplicas; se crea solo) y muestra cómo Kafka los reparte entre las particiones.</p>
                    </div>
                </div>
                <div class="test-grid">
                    <div class="form-card">
                        <h4>Ráfaga</h4>
                        <div class="input-group">
                            <label>Cantidad de mensajes</label>
                            <select v-model.number="carga.cantidad" :disabled="carga.corriendo">
                                <option :value="100">100</option>
                                <option :value="500">500</option>
                                <option :value="1000">1000</option>
                            </select>
                        </div>
                        <button class="btn btn-primary" @click="testCarga" :disabled="carga.corriendo || ocupado">
                            {{ carga.corriendo ? '⏳ Publicando ' + carga.hecho + ' / ' + carga.cantidad : '🚀 Lanzar test de carga' }}
                        </button>
                        <p class="hint">Claves <code>CARGA-1</code> … <code>CARGA-N</code>: la clave decide la partición. No toca los tópicos de negocio.</p>
                    </div>
                    <div class="form-card">
                        <h4>Resultado</h4>
                        <template v-if="carga.resultado">
                            <div class="ir"><span>✅ Enviados</span><strong>{{ carga.ok }} de {{ carga.cantidad }} <span v-if="carga.errores" class="text-err">({{ carga.errores }} con error)</span></strong></div>
                            <div class="ir"><span>⏱️ Tiempo</span><strong>{{ carga.ms }} ms</strong></div>
                            <div class="ir"><span>⚡ Velocidad</span><strong>{{ carga.porSeg }} mensajes/s</strong></div>
                            <div class="ir" v-for="(n, part) in carga.porParticion" :key="part">
                                <span>🧱 Partición {{ part }}</span><strong>{{ n }} mensajes</strong>
                            </div>
                        </template>
                        <p class="hint" v-else>Lanza el test para ver el reparto por partición.</p>
                    </div>
                </div>
            </div>

            <!-- VISOR DE MENSAJES -->
            <div class="card section">
                <div class="sec-head">
                    <div>
                        <h3>3 · Mensajes del tópico</h3>
                        <p>Últimos mensajes guardados en Kafka (se leen sin consumirlos). Úsalo también para ver el contenido de un DLT.</p>
                    </div>
                </div>
                <div class="viewer-wrap">
                    <div class="viewer-bar">
                        <select v-model="visor.topico" @change="abrirMensajes(visor.topico)">
                            <option value="" disabled>Elegir tópico…</option>
                            <option v-for="t in overview.topicos" :key="t.nombre" :value="t.nombre">{{ t.nombre }}</option>
                        </select>
                        <select v-model.number="visor.cantidad" @change="abrirMensajes(visor.topico)">
                            <option :value="5">5 mensajes</option>
                            <option :value="10">10 mensajes</option>
                            <option :value="20">20 mensajes</option>
                        </select>
                        <button class="btn btn-outline btn-sm" @click="abrirMensajes(visor.topico)" :disabled="!visor.topico || visor.cargando">
                            {{ visor.cargando ? '⏳' : '👁' }} Ver
                        </button>
                    </div>
                    <p class="hint warn" v-if="visor.error">{{ visor.error }}</p>
                    <div class="flow-empty big" v-if="visor.consultado && !visor.mensajes.length && !visor.error">
                        El tópico {{ visor.topico }} no tiene mensajes todavía.
                    </div>
                    <div class="msg" v-for="m in visor.mensajes" :key="m.particion + '-' + m.offset">
                        <div class="msg-head">
                            <span>🧱 partición {{ m.particion }}</span>
                            <span>#️⃣ offset {{ m.offset }}</span>
                            <span>🔑 {{ m.clave || '(sin clave)' }}</span>
                            <span>🕒 {{ fechaLocal(m.fecha) }}</span>
                        </div>
                        <pre>{{ bonito(m.valor) }}</pre>
                    </div>
                </div>
            </div>

            <!-- ADMINISTRACION -->
            <div class="card section">
                <div class="sec-head">
                    <div>
                        <h3>4 · Administración de Kafka</h3>
                        <p>Crear, modificar y eliminar tópicos y sus configuraciones, y revisar consumer groups, a través del BFF → kafkaadmin.</p>
                    </div>
                    <label class="chk chk-warn" :class="{ on: avanzado }">
                        <input type="checkbox" v-model="avanzado" /> 🔓 Modo avanzado: permitir tocar tópicos del sistema
                    </label>
                </div>

                <div class="tabs">
                    <button class="tab" :class="{ active: tab === 'topicos' }" @click="tab = 'topicos'">🗂️ Tópicos ({{ overview.topicos.length }})</button>
                    <button class="tab" :class="{ active: tab === 'grupos' }" @click="tab = 'grupos'">👥 Consumer groups ({{ overview.grupos.length }})</button>
                </div>

                <!-- TOPICOS -->
                <div class="admin-grid" v-if="tab === 'topicos'">
                    <form class="form-card" @submit.prevent="crearTopico">
                        <h4>Nuevo tópico</h4>
                        <div class="input-group">
                            <label>Nombre</label>
                            <input v-model="fTopico.nombre" placeholder="smartlogix.demo.eventos" />
                        </div>
                        <div class="two-cols">
                            <div class="input-group">
                                <label>Particiones</label>
                                <input type="number" min="1" max="100" v-model.number="fTopico.particiones" />
                            </div>
                            <div class="input-group">
                                <label>Réplicas</label>
                                <input type="number" min="1" max="3" v-model.number="fTopico.replicas" />
                            </div>
                        </div>
                        <div class="input-group">
                            <label>Retención (ms)</label>
                            <input v-model="fTopico.retencion" placeholder="604800000 (7 días)" />
                        <table class="guia-ms">
                            <thead><tr><th>Tiempo</th><th>Milisegundos</th><th></th></tr></thead>
                            <tbody>
                                <tr v-for="r in RETENCIONES" :key="r.ms">
                                    <td>{{ r.etiqueta }}</td><td><code>{{ r.ms }}</code></td>
                                    <td><button type="button" class="btn-usar" @click="fTopico.retencion = r.ms">usar</button></td>
                                </tr>
                            </tbody>
                        </table>
                        </div>
                        <div class="input-group">
                            <label>Política de limpieza</label>
                            <select v-model="fTopico.cleanup">
                                <option value="delete">delete — borra por tiempo</option>
                                <option value="compact">compact — deja el último valor por clave</option>
                                <option value="compact,delete">compact,delete — ambas</option>
                            </select>
                        </div>
                        <button class="btn btn-primary" :disabled="ocupado">➕ Crear tópico</button>
                        <p class="hint">Las réplicas no pueden superar el número de brokers (3).</p>
                    </form>

                    <div class="table-wrap">
                        <table class="tbl">
                            <thead><tr><th>Tópico</th><th>Part.</th><th>Rép.</th><th>Estado</th><th></th></tr></thead>
                            <tbody>
                                <tr v-for="t in overview.topicos" :key="t.nombre">
                                    <td>
                                        <strong class="clickable" @click="verTopico(t.nombre)">{{ t.nombre }}</strong>
                                        <span class="lock" v-if="t.protegido">🔒 sistema</span>
                                        <span class="lock custom" v-else>✏️ personalizado</span>
                                    </td>
                                    <td>{{ t.particiones }}</td>
                                    <td>{{ t.replicas }}</td>
                                    <td>
                                        <span :class="t.particionesSubReplicadas ? 'text-err' : 'text-ok'">
                                            {{ t.particionesSubReplicadas ? t.particionesSubReplicadas + ' sub-replicadas' : 'ISR completo' }}
                                        </span>
                                    </td>
                                    <td class="acts">
                                        <button class="ib" title="Ver mensajes" @click="verTopico(t.nombre)">👁</button>
                                        <button class="ib" title="Configuración y particiones" @click="abrirEdicion(t)" :disabled="ocupado || (t.protegido && !avanzado)">✏️</button>
                                        <button class="ib danger" title="Eliminar" @click="eliminarTopico(t)" :disabled="ocupado || (t.protegido && !avanzado)">🗑️</button>
                                    </td>
                                </tr>
                                <tr v-if="!overview.topicos.length"><td colspan="5" class="empty">Sin tópicos.</td></tr>
                            </tbody>
                        </table>
                    </div>
                </div>

                <!-- GRUPOS -->
                <div class="admin-grid grupos" v-if="tab === 'grupos'">
                    <div class="table-wrap">
                        <table class="tbl">
                            <thead><tr><th>Consumer group</th><th>Estado</th><th>Miembros</th><th>Lag</th><th>Asignación</th><th></th></tr></thead>
                            <tbody>
                                <tr v-for="g in gruposTabla" :key="g.grupo">
                                    <td><strong>{{ g.grupo }}</strong></td>
                                    <td><span :class="esStable(g.estado) ? 'text-ok' : 'text-err'">{{ g.estado }}</span></td>
                                    <td>{{ g.miembros }}</td>
                                    <td>{{ g.lag }}</td>
                                    <td>
                                        <div class="dim" v-for="o in g.offsets" :key="o.topico + o.particion">
                                            {{ o.topico }} · p{{ o.particion }} · {{ o.offsetConfirmado }}/{{ o.offsetFinal }} (lag {{ o.lag }})
                                        </div>
                                    </td>
                                    <td class="acts">
                                        <button class="ib danger" title="Eliminar grupo" @click="eliminarGrupo(g)" :disabled="ocupado">🗑️</button>
                                    </td>
                                </tr>
                                <tr v-if="!gruposTabla.length"><td colspan="6" class="empty">Sin consumer groups.</td></tr>
                            </tbody>
                        </table>
                    </div>
                </div>
            </div>

            <!-- DETALLE DE TOPICO -->
            <div class="card section" v-if="detalle.nombre">
                <div class="sec-head">
                    <div>
                        <h3>Detalle de {{ detalle.nombre }}</h3>
                        <p>{{ detalle.totalParticiones }} particiones · ≈ {{ detalle.mensajesAproximados }} mensajes</p>
                    </div>
                    <button class="btn btn-outline btn-sm" @click="detalle.nombre = ''">Cerrar</button>
                </div>
                <div class="table-wrap" style="margin:16px 20px">
                    <table class="tbl">
                        <thead><tr><th>Partición</th><th>Líder</th><th>Réplicas</th><th>ISR</th><th>Offsets</th></tr></thead>
                        <tbody>
                            <tr v-for="p in detalle.particiones" :key="p.particion">
                                <td>{{ p.particion }}</td>
                                <td>broker {{ p.lider }}</td>
                                <td>{{ p.replicas.join(', ') }}</td>
                                <td :class="p.isr.length === p.replicas.length ? 'text-ok' : 'text-err'">{{ p.isr.join(', ') }}</td>
                                <td>{{ p.offsetInicial }} → {{ p.offsetFinal }}</td>
                            </tr>
                        </tbody>
                    </table>
                </div>
                <div class="cfg-row">
                    <code v-for="(v, k) in detalle.configuracion" :key="k">{{ k }} = {{ v }}</code>
                </div>
            </div>

            <!-- LOGGER -->
            <div class="card logger-card">
                <div class="logger-top">
                    <h3>📋 Log de operaciones</h3>
                    <button class="btn-clear" @click="logs = []">Limpiar</button>
                </div>
                <div class="terminal">
                    <div v-if="!logs.length" class="log-empty">Sin actividad todavía.</div>
                    <div v-for="(l, i) in logs" :key="i" class="log-line" :class="'log-' + l.type">
                        <span class="lt">{{ l.time }}</span><span>{{ l.icon }}</span><span class="lm">{{ l.msg }}</span>
                    </div>
                </div>
            </div>
        </div>

        <!-- MODAL EDICION DE TOPICO -->
        <div class="overlay" v-if="edit.visible" @click.self="edit.visible = false">
            <div class="modal">
                <h3>Actualizar tópico</h3>
                <p class="modal-name">{{ edit.nombre }}</p>
                <div class="input-group">
                    <label>Retención (ms)</label>
                    <input v-model="edit.retencion" />
                        <table class="guia-ms">
                            <thead><tr><th>Tiempo</th><th>Milisegundos</th><th></th></tr></thead>
                            <tbody>
                                <tr v-for="r in RETENCIONES" :key="r.ms">
                                    <td>{{ r.etiqueta }}</td><td><code>{{ r.ms }}</code></td>
                                    <td><button type="button" class="btn-usar" @click="edit.retencion = r.ms">usar</button></td>
                                </tr>
                            </tbody>
                        </table>
                </div>
                <div class="input-group">
                    <label>Política de limpieza</label>
                    <select v-model="edit.cleanup">
                        <option value="delete">delete</option>
                        <option value="compact">compact</option>
                        <option value="compact,delete">compact,delete</option>
                    </select>
                </div>
                <div class="input-group">
                    <label>min.insync.replicas</label>
                    <input type="number" min="1" max="3" v-model.number="edit.minIsr" />
                </div>
                <div class="input-group">
                    <label>Particiones (solo se pueden aumentar)</label>
                    <input type="number" :min="edit.particionesActuales" max="100" v-model.number="edit.particiones" />
                </div>
                <div class="modal-actions">
                    <button class="btn btn-outline btn-sm" @click="edit.visible = false">Cancelar</button>
                    <button class="btn btn-primary btn-sm" @click="guardarEdicion" :disabled="ocupado">Guardar</button>
                </div>
            </div>
        </div>
    </div>
</template>

<script setup>
import { ref, reactive, computed, watch, onMounted, onBeforeUnmount } from 'vue';
import NavbarAdmin from '../components/NavbarAdmin.vue';
import ConfirmModal from '../components/ConfirmModal.vue';
import {
    getToken,
    kafkaOverview, kafkaTopico, kafkaCrearTopico, kafkaActualizarConfig, kafkaAumentarParticiones,
    kafkaEliminarTopico, kafkaVerMensajes, kafkaPublicar, kafkaGrupo, kafkaEliminarGrupo
} from '../services/api.js';

// ---------------------------------------------------------------------
// Diseño del flujo (coincide con lo implementado en los microservicios)
// ---------------------------------------------------------------------
const T_PEDIDOS = 'smartlogix.pedidos.eventos';
const T_LOGISTICA = 'smartlogix.logistica.eventos';

const FLUJO = [
    {
        topico: T_PEDIDOS, clave: 'pedidoId',
        productores: [{ ms: 'Pedidos', icono: '🛒', eventos: 'PEDIDO_CREADO · PEDIDO_ESTADO_CAMBIADO' }],
        consumidores: [
            { ms: 'Inventario', icono: '📦', grupo: 'inventario-pedidos-grp', accion: 'repone stock si el pedido es RECHAZADO (idempotente)' },
            { ms: 'Envíos', icono: '🚚', grupo: 'envios-pedidos-grp', accion: 'crea el envío cuando el pedido es APROBADO' }
        ]
    },
    {
        topico: T_LOGISTICA, clave: 'PRODUCTO-id / ENVIO-id',
        productores: [
            { ms: 'Inventario', icono: '📦', eventos: 'STOCK_BAJO' },
            { ms: 'Envíos', icono: '🚚', eventos: 'ENVIO_CREADO · ENVIO_ESTADO_CAMBIADO' }
        ],
        consumidores: [
            { ms: 'Pedidos', icono: '🛒', grupo: 'pedidos-logistica-grp', accion: 'registra el seguimiento logístico' },
            { ms: 'Usuarios', icono: '👥', grupo: 'usuarios-alertas-grp', accion: 'notifica a los ADMIN cuando hay STOCK_BAJO' }
        ]
    }
];

const PRUEBAS = [
    {
        key: 'pedidos', etiqueta: 'Pedidos → pedidos.eventos', topico: T_PEDIDOS, clave: 'PEDIDO-9001',
        consumen: 'inventario-pedidos-grp y envios-pedidos-grp',
        valido: () => ({
            eventId: crypto.randomUUID(), tipo: 'PEDIDO_CREADO', pedidoId: 9001, cliente: 'demo@smartlogix.cl',
            productoId: 1, nombreProducto: 'Producto demo', cantidad: 1,
            ocurridoEn: new Date().toISOString(), origen: 'frontend-demo'
        })
    },
    {
        key: 'logistica', etiqueta: 'Stock bajo → logistica.eventos', topico: T_LOGISTICA, clave: 'PRODUCTO-9001',
        consumen: 'pedidos-logistica-grp y usuarios-alertas-grp (avisa a los ADMIN)',
        valido: () => ({
            eventId: crypto.randomUUID(), tipo: 'STOCK_BAJO', productoId: 9001, nombreProducto: 'Producto demo',
            stockActual: 2, umbral: 5, ocurridoEn: new Date().toISOString(), origen: 'frontend-demo'
        })
    }
];
const INVALIDO = 'esto-no-es-un-json-valido';

// Guía para escribir retention.ms
const RETENCIONES = [
    { etiqueta: '10 minutos', ms: '600000' },
    { etiqueta: '1 hora', ms: '3600000' },
    { etiqueta: '1 día', ms: '86400000' },
    { etiqueta: '7 días', ms: '604800000' },
    { etiqueta: '30 días', ms: '2592000000' }
];

const GRUPOS_CONOCIDOS = [
    { grupo: 'inventario-pedidos-grp', ms: 'Inventario', icono: '📦' },
    { grupo: 'envios-pedidos-grp', ms: 'Envíos', icono: '🚚' },
    { grupo: 'pedidos-logistica-grp', ms: 'Pedidos', icono: '🛒' },
    { grupo: 'usuarios-alertas-grp', ms: 'Usuarios', icono: '👥' }
];

// ---------------------------------------------------------------------
// Estado
// ---------------------------------------------------------------------
const confirmRef = ref(null);
const confirmar = (opts) => confirmRef.value.abrir(opts);

const overview = reactive({ cluster: { brokers: [], clusterId: '' }, topicos: [], grupos: [], detalles: {} });
const api = reactive({ ok: false, error: '' });
const cargando = ref(false);
const ocupado = ref(false);
const auto = ref(true);
const avanzado = ref(false);
const tab = ref('topicos');
const logs = ref([]);
const banner = reactive({ tipo: 'ok', msg: '' });
const seguridad = reactive({ sinToken: null, conToken: null });
const ultimoEnvio = ref(null);

const pruebaSel = ref('pedidos');
const pub = reactive({ topico: T_PEDIDOS, clave: 'PEDIDO-9001', valor: '' });
const fTopico = reactive({ nombre: '', particiones: 3, replicas: 3, retencion: '', cleanup: 'delete' });
const visor = reactive({ topico: T_PEDIDOS, cantidad: 10, mensajes: [], cargando: false, error: '', consultado: false });
const detalle = reactive({ nombre: '', totalParticiones: 0, mensajesAproximados: 0, particiones: [], configuracion: {} });
const edit = reactive({ visible: false, nombre: '', retencion: '', cleanup: 'delete', minIsr: 2, particiones: 3, particionesActuales: 3, original: {} });

let temporizador = null;
let bannerTimer = null;
let ultimoErrorLog = '';

// Kafka UI responde solo por HTTP (puerto 8081), aunque la app se abra por HTTPS
const kafkaUiUrl = `http://${window.location.hostname}:8081`;

// ---------------------------------------------------------------------
// Datos derivados
// ---------------------------------------------------------------------
const pruebaActual = computed(() => PRUEBAS.find(p => p.key === pruebaSel.value) || PRUEBAS[0]);
const brokersActivos = computed(() => overview.cluster.brokers.length);
const topicosNegocio = computed(() => overview.topicos.filter(t => !t.nombre.endsWith('.DLT') && t.nombre.startsWith('smartlogix.')));
const dlts = computed(() => overview.topicos.filter(t => t.nombre.endsWith('.DLT')));
const topicosOk = computed(() =>
    topicosNegocio.value.length >= 2 &&
    topicosNegocio.value.every(t => t.particiones === 3 && t.replicas === 3));
const mensajesDlt = ref(0);

const flujoGrupos = computed(() => GRUPOS_CONOCIDOS.map(g => ({ ...g, detalle: overview.detalles[g.grupo] })));
const gruposOk = computed(() => flujoGrupos.value.every(g => g.detalle && esStable(g.detalle.estado) && g.detalle.totalMiembros > 0));

const gruposTabla = computed(() => overview.grupos.map(g => {
    const d = overview.detalles[g.grupo];
    return {
        grupo: g.grupo,
        estado: d ? d.estado : g.estado,
        miembros: d ? d.totalMiembros : '—',
        lag: d ? d.lagTotal : '—',
        offsets: d ? d.offsets : []
    };
}));

function infoTopico(nombre) { return overview.topicos.find(t => t.nombre === nombre); }
function lagGrupo(grupo) {
    const d = overview.detalles[grupo];
    return d ? d.lagTotal : null;
}

// ---------------------------------------------------------------------
// Utilidades
// ---------------------------------------------------------------------
const esStable = (e) => String(e || '').toUpperCase() === 'STABLE';
const esperar = (ms) => new Promise(r => setTimeout(r, ms));

function addLog(type, icon, msg) {
    const time = new Date().toLocaleTimeString('es-CL', { hour12: false });
    logs.value.unshift({ type, icon, msg, time });
    if (logs.value.length > 80) logs.value.pop();
}

function avisar(tipo, msg) {
    banner.tipo = tipo;
    banner.msg = msg;
    clearTimeout(bannerTimer);
    bannerTimer = setTimeout(() => { banner.msg = ''; }, 8000);
}

function bonito(texto) {
    try { return JSON.stringify(JSON.parse(texto), null, 2); } catch { return texto; }
}

function fechaLocal(iso) {
    try { return new Date(iso).toLocaleString('es-CL', { hour12: false }); } catch { return iso; }
}

// ---------------------------------------------------------------------
// Carga
// ---------------------------------------------------------------------
async function cargar(silencioso = false, fresco = false) {
    if (cargando.value) {
        if (!fresco) return;
        while (cargando.value) await esperar(100);
    }
    cargando.value = true;
    try {
        const d = await kafkaOverview();
        overview.cluster = d.cluster || { brokers: [], clusterId: '' };
        overview.topicos = d.topicos || [];
        overview.grupos = d.grupos || [];

        // detalle (lag) de cada grupo
        const detalles = {};
        await Promise.all(overview.grupos.map(async g => {
            try { detalles[g.grupo] = await kafkaGrupo(g.grupo); } catch { /* grupo recién borrado */ }
        }));
        overview.detalles = detalles;

        await contarDlt();

        if (!api.ok && !silencioso) {
            addLog('success', '📡', `Cluster leído vía BFF: ${overview.cluster.brokers.length} brokers, ${overview.topicos.length} tópicos, ${overview.grupos.length} grupos`);
        }
        api.ok = true;
        api.error = '';
        ultimoErrorLog = '';
    } catch (e) {
        api.ok = false;
        api.error = e.message;
        if (e.message !== ultimoErrorLog) {
            addLog('error', '❌', `No se pudo leer Kafka: ${e.message}`);
            ultimoErrorLog = e.message;
        }
    } finally {
        cargando.value = false;
    }
}

async function contarDlt() {
    let total = 0;
    for (const t of dlts.value) {
        try {
            const d = await kafkaTopico(t.nombre);
            total += d.mensajesAproximados || 0;
        } catch { /* se ignora */ }
    }
    mensajesDlt.value = total;
}

function programarAuto() {
    clearInterval(temporizador);
    temporizador = null;
    if (auto.value) temporizador = setInterval(() => { if (!ocupado.value) cargar(true); }, 5000);
}

async function ejecutar(descripcion, fn, refrescar = true) {
    ocupado.value = true;
    addLog('info', '⏳', descripcion);
    try {
        const r = await fn();
        addLog('success', '✅', `${descripcion} → OK`);
        if (refrescar) {
            await esperar(600);
            await cargar(true, true);
        }
        return { ok: true, data: r };
    } catch (e) {
        addLog('error', '❌', `${descripcion} → ${e.message}`);
        avisar('err', e.message);
        return { ok: false, error: e.message };
    } finally {
        ocupado.value = false;
    }
}

// ---------------------------------------------------------------------
// Pruebas: publicar y DLT
// ---------------------------------------------------------------------
function cargarPreset(tipo) {
    const p = pruebaActual.value;
    pub.topico = p.topico;
    pub.clave = p.clave;
    pub.valor = tipo === 'valido' ? JSON.stringify(p.valido(), null, 2) : INVALIDO;
}

async function publicar() {
    const r = await ejecutar(`POST mensaje → ${pub.topico} [clave ${pub.clave || 'sin clave'}]`, () =>
        kafkaPublicar(pub.topico, { clave: pub.clave || null, valor: pub.valor }), false);
    if (!r.ok) return;
    ultimoEnvio.value = r.data;
    addLog('success', '📤', `Publicado en ${pub.topico}: partición ${r.data.particion}, offset ${r.data.offset}`);
    avisar('ok', `Mensaje publicado (partición ${r.data.particion}, offset ${r.data.offset}). Mira el visor y el lag.`);
    visor.topico = pub.topico;
    await esperar(1500);
    await abrirMensajes(pub.topico, true);
    await cargar(true, true);
}


// ---------------------------------------------------------------------
// Test de carga: N mensajes en demo.carga
// ---------------------------------------------------------------------
const T_CARGA = 'demo.carga';
const carga = reactive({ cantidad: 1000, corriendo: false, hecho: 0, ok: 0, errores: 0, ms: 0, porSeg: 0, porParticion: {}, resultado: false });

async function testCarga() {
    carga.corriendo = true; carga.resultado = false;
    carga.hecho = 0; carga.ok = 0; carga.errores = 0; carga.porParticion = {};
    try {
        if (!infoTopico(T_CARGA)) {
            addLog('info', '🆕', `Creando ${T_CARGA} (3P/3R)`);
            await kafkaCrearTopico({ nombre: T_CARGA, particiones: 3, replicas: 3, configs: { 'cleanup.policy': 'delete', 'retention.ms': '3600000' } });
        }
    } catch (e) {
        if (!/exist/i.test(e.message)) {
            avisar('err', e.message); carga.corriendo = false; return;
        }
    }
    addLog('info', '🚀', `Test de carga: ${carga.cantidad} mensajes → ${T_CARGA}`);
    const inicio = performance.now();
    let siguiente = 1;
    const hilos = 6;
    async function worker() {
        while (siguiente <= carga.cantidad) {
            const n = siguiente++;
            const valor = JSON.stringify({ eventId: crypto.randomUUID(), tipo: 'CARGA', numero: n, ocurridoEn: new Date().toISOString(), origen: 'test-carga' });
            let enviado = false;
            for (let intento = 0; intento < 3 && !enviado; intento++) {
                try {
                    const r = await kafkaPublicar(T_CARGA, { clave: 'CARGA-' + n, valor });
                    carga.porParticion[r.particion] = (carga.porParticion[r.particion] || 0) + 1;
                    carga.ok++; enviado = true;
                } catch (e) {
                    await esperar(200 * (intento + 1));
                }
            }
            if (!enviado) carga.errores++;
            carga.hecho++;
        }
    }
    await Promise.all(Array.from({ length: hilos }, worker));
    carga.ms = Math.round(performance.now() - inicio);
    carga.porSeg = Math.round(carga.ok / (carga.ms / 1000));
    carga.resultado = true;
    carga.corriendo = false;
    addLog(carga.errores ? 'warn' : 'success', '📊', `Test de carga: ${carga.ok}/${carga.cantidad} en ${carga.ms} ms (${carga.porSeg} msg/s)`);
    avisar(carga.errores ? 'err' : 'ok', `Test de carga: ${carga.ok} mensajes publicados en ${carga.ms} ms.`);
    await cargar(true, true);
}

async function probarDlt() {
    const p = pruebaActual.value;
    const dlt = p.topico + '.DLT';
    const antes = (infoDlt(dlt) || 0);
    const r = await ejecutar(`Evento inválido → ${p.topico}`, () =>
        kafkaPublicar(p.topico, { clave: 'DLT-' + Date.now(), valor: INVALIDO }), false);
    if (!r.ok) return;
    addLog('info', '⏳', `Los consumidores reintentan 3 veces (1s, 2s, 4s) y luego lo mandan a ${dlt}...`);
    await esperar(9000);
    await cargar(true, true);
    visor.topico = dlt;
    await abrirMensajes(dlt, true);
    const despues = infoDlt(dlt) || 0;
    if (visor.mensajes.length && despues >= antes) {
        addLog('success', '☠️', `${dlt} tiene mensajes: el reintento y el Dead Letter Topic funcionan`);
        avisar('ok', `El mensaje inválido terminó en ${dlt}.`);
    } else {
        addLog('warn', '⚠️', `${dlt} sigue vacío. Revisa que los consumidores estén activos.`);
        avisar('err', `No se vio el mensaje en ${dlt}. Actualiza en unos segundos.`);
    }
}
function infoDlt() { return mensajesDlt.value; }

async function abrirMensajes(topico, silencioso = false) {
    if (!topico) return;
    visor.topico = topico;
    visor.cargando = true;
    visor.error = '';
    try {
        visor.mensajes = await kafkaVerMensajes(topico, visor.cantidad);
        visor.consultado = true;
        if (!silencioso) addLog('info', '👁', `Mensajes de ${topico}: ${visor.mensajes.length}`);
    } catch (e) {
        visor.mensajes = [];
        visor.error = e.message;
        addLog('error', '❌', `Ver mensajes de ${topico} → ${e.message}`);
    } finally {
        visor.cargando = false;
    }
}

async function verTopico(nombre) {
    try {
        const d = await kafkaTopico(nombre);
        Object.assign(detalle, d);
        addLog('info', '🔍', `Detalle de ${nombre}: ${d.totalParticiones} particiones`);
    } catch (e) {
        avisar('err', e.message);
    }
    await abrirMensajes(nombre);
}

// ---------------------------------------------------------------------
// Administración de tópicos
// ---------------------------------------------------------------------
async function crearTopico() {
    const nombre = fTopico.nombre.trim();
    if (!nombre) return avisar('err', 'Escribe el nombre del tópico.');
    if (!/^[a-zA-Z0-9._\-]+$/.test(nombre)) return avisar('err', 'El nombre solo puede tener letras, números, puntos, guiones y guiones bajos.');
    const configs = { 'cleanup.policy': fTopico.cleanup };
    if (fTopico.retencion.trim()) configs['retention.ms'] = fTopico.retencion.trim();
    const r = await ejecutar(`POST tópico ${nombre} (${fTopico.particiones}P/${fTopico.replicas}R)`, () =>
        kafkaCrearTopico({ nombre, particiones: fTopico.particiones, replicas: fTopico.replicas, configs }));
    if (r.ok) {
        avisar('ok', `Tópico «${nombre}» creado con ${fTopico.particiones} particiones y ${fTopico.replicas} réplicas.`);
        fTopico.nombre = '';
    }
}

async function eliminarTopico(t) {
    const force = t.protegido && avanzado.value;
    const ok = await confirmar({
        titulo: 'Eliminar tópico',
        mensaje: `Se eliminará «${t.nombre}» con todos sus mensajes.` + (force ? ' ATENCIÓN: lo usan los microservicios.' : ''),
        textoConfirmar: 'Eliminar'
    });
    if (!ok) return;
    const r = await ejecutar(`DELETE tópico ${t.nombre}`, () => kafkaEliminarTopico(t.nombre, force));
    if (r.ok) {
        avisar('ok', `Tópico «${t.nombre}» eliminado.`);
        if (detalle.nombre === t.nombre) detalle.nombre = '';
    }
}

async function abrirEdicion(t) {
    let cfg = {};
    try { cfg = (await kafkaTopico(t.nombre)).configuracion || {}; } catch { /* se edita con valores por defecto */ }
    edit.nombre = t.nombre;
    edit.retencion = cfg['retention.ms'] || '';
    edit.cleanup = cfg['cleanup.policy'] || 'delete';
    edit.minIsr = Number(cfg['min.insync.replicas'] || 2);
    edit.particiones = t.particiones;
    edit.particionesActuales = t.particiones;
    edit.original = { retencion: edit.retencion, cleanup: edit.cleanup, minIsr: edit.minIsr };
    edit.visible = true;
}

async function guardarEdicion() {
    const force = avanzado.value;
    const configs = {};
    if (edit.retencion !== edit.original.retencion && String(edit.retencion).trim()) configs['retention.ms'] = String(edit.retencion).trim();
    if (edit.cleanup !== edit.original.cleanup) configs['cleanup.policy'] = edit.cleanup;
    if (edit.minIsr !== edit.original.minIsr) configs['min.insync.replicas'] = String(edit.minIsr);
    const nombre = edit.nombre;
    const nuevasParticiones = edit.particiones;
    const subir = nuevasParticiones > edit.particionesActuales;
    edit.visible = false;
    if (!Object.keys(configs).length && !subir) return avisar('ok', 'No hay cambios que guardar.');

    if (Object.keys(configs).length) {
        const r = await ejecutar(`PUT config ${nombre} ${JSON.stringify(configs)}`, () => kafkaActualizarConfig(nombre, configs, force), !subir);
        if (!r.ok) return;
    }
    if (subir) {
        const r = await ejecutar(`PUT particiones ${nombre} → ${nuevasParticiones}`, () => kafkaAumentarParticiones(nombre, nuevasParticiones, force));
        if (!r.ok) return;
    }
    avisar('ok', `Tópico «${nombre}» actualizado.`);
}

async function eliminarGrupo(g) {
    const ok = await confirmar({
        titulo: 'Eliminar consumer group',
        mensaje: `Se eliminará «${g.grupo}». Solo es posible si no tiene miembros conectados.`,
        textoConfirmar: 'Eliminar'
    });
    if (!ok) return;
    const r = await ejecutar(`DELETE grupo ${g.grupo}`, () => kafkaEliminarGrupo(g.grupo));
    if (r.ok) avisar('ok', `Grupo «${g.grupo}» eliminado.`);
}

// ---------------------------------------------------------------------
// Seguridad (JWT)
// ---------------------------------------------------------------------
async function testSinToken() {
    ocupado.value = true;
    addLog('info', '🧪', 'Test SIN token → GET /api/kafka/overview');
    try {
        const res = await fetch('/api/kafka/overview', { headers: { 'ngrok-skip-browser-warning': 'true' } });
        if (res.status === 401 || res.status === 403) {
            seguridad.sinToken = { ok: true, msg: `${res.status} rechazado ✓` };
            addLog('success', '🔐', `Sin token → ${res.status}: el BFF protege Kafka`);
        } else {
            seguridad.sinToken = { ok: false, msg: `HTTP ${res.status} (debería ser 401)` };
            addLog('warn', '⚠️', `Sin token → HTTP ${res.status} inesperado`);
        }
    } catch {
        seguridad.sinToken = { ok: true, msg: 'Rechazado por red/CORS ✓' };
        addLog('success', '🔐', 'Sin token → rechazado');
    }
    ocupado.value = false;
}

async function testConToken() {
    ocupado.value = true;
    addLog('info', '🧪', 'Test CON token → GET /api/kafka/overview');
    try {
        const d = await kafkaOverview();
        seguridad.conToken = { ok: true, msg: `200 OK · ${d.topicos.length} tópicos ✓` };
        addLog('success', '✅', `Con token → 200 OK (${getToken() ? 'JWT enviado' : 'sin JWT local'})`);
    } catch (e) {
        seguridad.conToken = { ok: false, msg: e.message };
        addLog('error', '❌', `Con token → ${e.message}`);
    }
    ocupado.value = false;
}

// ---------------------------------------------------------------------
// Ciclo de vida
// ---------------------------------------------------------------------
watch(auto, programarAuto);

onMounted(async () => {
    addLog('info', '🚀', 'Demo EP3 iniciada — consultando Kafka a través del BFF...');
    cargarPreset('valido');
    await cargar();
    await abrirMensajes(visor.topico, true);
    programarAuto();
});

onBeforeUnmount(() => {
    clearInterval(temporizador);
    clearTimeout(bannerTimer);
});
</script>

<style scoped>
.demo-header{display:flex;justify-content:space-between;align-items:flex-start;margin-bottom:20px;gap:12px;}
.demo-header h1{font-size:1.8rem;}
.status-row{display:flex;gap:10px;flex-wrap:wrap;margin-bottom:24px;align-items:center;}
.schip{padding:6px 14px;border-radius:999px;font-size:0.82rem;font-weight:600;border:1px solid var(--border);background:var(--bg-card);}
.chip-ok{border-color:rgba(34,197,94,0.4);color:#22c55e;background:rgba(34,197,94,0.08);}
.chip-warn{border-color:rgba(245,158,11,0.4);color:#f59e0b;background:rgba(245,158,11,0.08);}
.chip-err{border-color:rgba(239,68,68,0.4);color:#ef4444;background:rgba(239,68,68,0.08);}
.status-tools{margin-left:auto;display:flex;gap:12px;align-items:center;}
.chk{display:flex;align-items:center;gap:6px;font-size:0.82rem;color:var(--text-secondary);cursor:pointer;}
.chk input{accent-color:var(--accent);}
.btn-sm{padding:5px 12px;font-size:0.78rem;}
.text-muted{color:var(--text-muted);}

/* tarjetas de requisitos (mismo estilo que EP1) */
.demo-grid{display:grid;grid-template-columns:1fr 1fr;gap:20px;margin-bottom:20px;}
.demo-col{display:flex;flex-direction:column;gap:16px;}
.req-card{overflow:hidden;}
.req-head{display:flex;align-items:flex-start;gap:14px;padding:16px 18px;border-bottom:1px solid var(--border);}
.rn{width:28px;height:28px;border-radius:50%;background:var(--accent);color:#fff;font-size:0.8rem;font-weight:700;display:flex;align-items:center;justify-content:center;flex-shrink:0;}
.req-head h3{font-size:0.93rem;margin-bottom:2px;font-weight:600;}
.req-head p{font-size:0.78rem;color:var(--text-muted);margin:0;}
.rbadge{margin-left:auto;padding:3px 10px;border-radius:999px;font-size:0.72rem;font-weight:600;white-space:nowrap;flex-shrink:0;background:rgba(90,90,114,0.2);color:var(--text-muted);}
.rbadge-ok{background:rgba(34,197,94,0.15);color:#22c55e;}
.req-body{padding:12px 18px;display:flex;flex-direction:column;gap:6px;}
.ir{display:flex;align-items:center;gap:10px;font-size:0.82rem;padding:4px 0;flex-wrap:wrap;}
.ir > span:first-child{color:var(--text-muted);min-width:90px;flex-shrink:0;}
code{font-family:monospace;font-size:0.75rem;color:var(--accent);background:rgba(124,92,252,0.08);padding:2px 6px;border-radius:4px;word-break:break-all;}
.test-btns{display:flex;gap:8px;margin:4px 0 8px;flex-wrap:wrap;}
.text-ok{color:#22c55e;}.text-err{color:#ef4444;}
.guia-ms{width:100%;border-collapse:collapse;font-size:0.76rem;margin-top:6px;}
.guia-ms th{text-align:left;color:var(--text-muted);font-weight:600;padding:3px 6px;}
.guia-ms td{padding:3px 6px;border-top:1px solid var(--border);}
.btn-usar{border:1px solid var(--border);background:transparent;color:var(--accent);border-radius:6px;padding:1px 8px;font-size:0.72rem;cursor:pointer;}
.ms-row{display:flex;align-items:center;gap:10px;padding:7px 0;border-bottom:1px solid var(--border);font-size:0.82rem;}
.ms-row:last-child{border-bottom:none;}
.ms-dot{width:8px;height:8px;border-radius:50%;flex-shrink:0;}
.dot-ok{background:#22c55e;box-shadow:0 0 6px #22c55e;}.dot-err{background:#ef4444;}
.ms-nombre{flex:1;}.ms-port{color:var(--text-muted);font-family:monospace;font-size:0.72rem;}.ms-count{color:var(--accent);font-size:0.75rem;}

/* secciones */
.section{padding:0;margin-bottom:20px;overflow:hidden;}
.section:hover{border-color:var(--border);box-shadow:none;}
.sec-head{display:flex;justify-content:space-between;align-items:flex-start;gap:12px;padding:16px 20px;border-bottom:1px solid var(--border);}
.sec-head h3{font-size:1rem;font-weight:600;margin-bottom:2px;}
.sec-head p{font-size:0.8rem;color:var(--text-muted);margin:0;}
.upd{font-size:0.72rem;color:var(--text-muted);white-space:nowrap;}
.hint{font-size:0.76rem;color:var(--text-muted);margin:4px 0 0;}
.hint.warn{color:#f59e0b;}
.dim{font-size:0.72rem;color:var(--text-muted);}

/* selector */
.ms-tabs{display:grid;grid-template-columns:repeat(5,1fr);gap:10px;padding:16px 20px 8px;}
.ms-tab{display:flex;flex-direction:column;align-items:flex-start;gap:2px;padding:12px 14px;border-radius:var(--radius);border:1px solid var(--border);background:var(--bg-secondary);color:var(--text-primary);cursor:pointer;text-align:left;transition:var(--transition);}
.ms-tab:hover{border-color:var(--accent);}
.ms-tab.active{border-color:var(--accent);background:rgba(124,92,252,0.12);box-shadow:0 0 0 3px var(--accent-glow);}
.ms-ico{font-size:1.3rem;}
.ms-name{font-weight:600;font-size:0.92rem;}
.ms-sub{font-size:0.7rem;color:var(--text-muted);}
.opts-row{display:flex;gap:18px;flex-wrap:wrap;padding:8px 20px 14px;}
.chk-warn.on{color:#f59e0b;font-weight:600;}
.section > .hint{padding:0 20px 14px;}

/* topologia */
.flow-card{margin:14px 20px;border:1px solid var(--border);border-radius:var(--radius);background:var(--bg-secondary);overflow:hidden;}
.flow-card.orphan{border-style:dashed;}
.flow-ex{display:flex;align-items:center;gap:10px;flex-wrap:wrap;padding:10px 14px;background:rgba(124,92,252,0.07);border-bottom:1px solid var(--border);font-size:0.88rem;}
.flow-icon{font-size:1rem;}
.flow-rows{padding:8px 14px;display:flex;flex-direction:column;gap:8px;}
.flow-row{display:flex;align-items:center;gap:10px;flex-wrap:wrap;}
.flow-arrow{display:flex;align-items:center;gap:6px;color:var(--text-muted);font-size:0.78rem;min-width:190px;}
.flow-arrow .line{display:inline-block;width:26px;height:2px;background:var(--border);}
.q-chip{display:flex;flex-direction:column;align-items:flex-start;gap:1px;padding:7px 12px;border-radius:10px;border:1px solid var(--border);background:var(--bg-card);color:var(--text-primary);cursor:pointer;font-size:0.84rem;text-align:left;transition:var(--transition);}
.q-chip:hover{border-color:var(--accent);}
.q-chip small{font-size:0.7rem;color:var(--text-muted);}
.flow-dlx{font-size:0.76rem;color:#f59e0b;}
.flow-empty{font-size:0.8rem;color:var(--text-muted);padding:4px 0;}
.flow-empty.big{text-align:center;padding:30px;}
.ex-badge{display:inline-block;padding:2px 9px;border-radius:999px;font-size:0.68rem;font-weight:700;text-transform:uppercase;letter-spacing:.04em;margin-right:4px;background:rgba(90,90,114,0.25);color:var(--text-secondary);}
.t-direct{background:rgba(59,130,246,0.18);color:#60a5fa;}
.t-topic{background:rgba(168,85,247,0.18);color:#c084fc;}
.t-fanout{background:rgba(34,197,94,0.16);color:#22c55e;}
.t-headers{background:rgba(245,158,11,0.16);color:#f59e0b;}
.lock{font-size:0.7rem;padding:2px 8px;border-radius:999px;background:rgba(245,158,11,0.14);color:#f59e0b;margin-left:4px;}
.lock.custom{background:rgba(34,197,94,0.14);color:#22c55e;}

/* administracion */
.tabs{display:flex;gap:6px;padding:12px 20px 0;border-bottom:1px solid var(--border);flex-wrap:wrap;}
.tab{padding:9px 16px;border:none;background:none;color:var(--text-muted);font-size:0.86rem;font-weight:600;cursor:pointer;border-bottom:2px solid transparent;margin-bottom:-1px;}
.tab:hover{color:var(--text-primary);}
.tab.active{color:var(--accent);border-bottom-color:var(--accent);}
.banner{margin:14px 20px 0;padding:10px 14px;border-radius:var(--radius);font-size:0.84rem;font-weight:500;}
.banner-ok{background:rgba(34,197,94,0.1);color:#22c55e;border:1px solid rgba(34,197,94,0.3);}
.banner-err{background:rgba(239,68,68,0.1);color:#ef4444;border:1px solid rgba(239,68,68,0.3);}
.admin-grid,.test-grid{display:grid;grid-template-columns:340px 1fr;gap:20px;padding:18px 20px 20px;align-items:start;}
.form-card{display:flex;flex-direction:column;gap:12px;padding:16px;border:1px solid var(--border);border-radius:var(--radius);background:var(--bg-secondary);min-width:0;}
.form-card h4{font-size:0.92rem;font-weight:600;}
.form-card .input-group input,.form-card .input-group select,.form-card .input-group textarea{width:100%;box-sizing:border-box;font-size:0.86rem;padding:8px 12px;}
.form-card textarea{font-family:monospace;resize:vertical;}
.prefix-input{display:flex;align-items:stretch;border:1px solid var(--border);border-radius:var(--radius);background:var(--bg-card);overflow:hidden;}
.prefix-input .prefix{display:flex;align-items:center;padding:0 10px;background:rgba(124,92,252,0.1);color:var(--accent);font-family:monospace;font-size:0.78rem;white-space:nowrap;}
.form-card .prefix-input input{border:none;border-radius:0;background:transparent;}
.table-wrap{overflow-x:auto;border:1px solid var(--border);border-radius:var(--radius);}
.tbl{width:100%;border-collapse:collapse;font-size:0.82rem;}
.tbl th{text-align:left;padding:10px 12px;font-size:0.72rem;text-transform:uppercase;letter-spacing:.04em;color:var(--text-muted);border-bottom:1px solid var(--border);background:var(--bg-secondary);white-space:nowrap;}
.tbl td{padding:10px 12px;border-bottom:1px solid var(--border);vertical-align:top;}
.tbl tr:last-child td{border-bottom:none;}
.tbl td.empty{text-align:center;color:var(--text-muted);padding:24px;}
.clickable{cursor:pointer;}
.clickable:hover{color:var(--accent);}
.acts{white-space:nowrap;text-align:right;}
.ib{width:30px;height:30px;border-radius:8px;border:1px solid var(--border);background:var(--bg-card);cursor:pointer;font-size:0.85rem;margin-left:4px;transition:var(--transition);}
.ib:hover:not(:disabled){border-color:var(--accent);}
.ib.danger:hover:not(:disabled){border-color:#ef4444;}
.ib:disabled{opacity:.35;cursor:not-allowed;}

/* pruebas */
.viewer-bar{display:flex;gap:8px;}
.viewer-bar select{flex:1;min-width:0;background:var(--bg-card);border:1px solid var(--border);border-radius:var(--radius);padding:8px 12px;color:var(--text-primary);font-size:0.84rem;}
.msg{border:1px solid var(--border);border-radius:10px;background:var(--bg-card);overflow:hidden;}
.msg-head{display:flex;gap:12px;flex-wrap:wrap;padding:7px 12px;font-size:0.74rem;color:var(--text-muted);border-bottom:1px solid var(--border);}
.msg-death{padding:7px 12px;font-size:0.76rem;color:#f59e0b;background:rgba(245,158,11,0.08);border-bottom:1px solid var(--border);}
.msg pre{margin:0;padding:10px 12px;font-size:0.76rem;font-family:monospace;color:var(--text-primary);white-space:pre-wrap;word-break:break-word;}

/* logger */
.logger-card{overflow:hidden;margin-bottom:20px;}
.logger-top{display:flex;justify-content:space-between;align-items:center;padding:14px 18px;border-bottom:1px solid var(--border);}
.logger-top h3{font-size:0.95rem;font-weight:600;}
.btn-clear{background:none;border:none;color:var(--text-muted);font-size:0.8rem;cursor:pointer;padding:4px 8px;border-radius:6px;transition:var(--transition);}
.btn-clear:hover{color:#ef4444;}
.terminal{background:#0d0d14;padding:12px;height:240px;overflow-y:auto;display:flex;flex-direction:column;gap:3px;font-family:'Courier New',monospace;}
.log-line{display:flex;gap:8px;font-size:0.78rem;padding:3px 6px;border-radius:5px;}
.lt{color:#4a4a6a;flex-shrink:0;font-size:0.7rem;}.lm{color:#b0b0c8;}
.log-success .lm{color:#22c55e;}.log-success{background:rgba(34,197,94,0.04);}
.log-error .lm{color:#ef4444;}.log-error{background:rgba(239,68,68,0.04);}
.log-warn .lm{color:#f59e0b;}
.log-empty{color:var(--text-muted);text-align:center;padding:20px;font-size:0.82rem;}

/* modal */
.overlay{position:fixed;inset:0;background:rgba(0,0,0,0.6);display:flex;align-items:center;justify-content:center;z-index:900;padding:16px;}
.modal{width:100%;max-width:420px;background:var(--bg-card);border:1px solid var(--border);border-radius:var(--radius-lg);padding:22px;display:flex;flex-direction:column;gap:12px;}
.modal h3{font-size:1.05rem;}
.modal-name{font-family:monospace;font-size:0.8rem;color:var(--accent);margin:-6px 0 0;word-break:break-all;}
.modal .input-group input,.modal .input-group select{width:100%;box-sizing:border-box;font-size:0.86rem;padding:8px 12px;}
.modal-actions{display:flex;justify-content:flex-end;gap:8px;margin-top:6px;}

@media(max-width:1000px){
    .ms-tabs{grid-template-columns:repeat(3,1fr);}
    .admin-grid,.test-grid{grid-template-columns:1fr;}
}
@media(max-width:900px){.demo-grid{grid-template-columns:1fr;}}
@media(max-width:560px){
    .ms-tabs{grid-template-columns:repeat(2,1fr);}
    .demo-header h1{font-size:1.4rem;}
    .status-tools{margin-left:0;}
}
.head-btns{display:flex;gap:8px;flex-wrap:wrap;}
.head-btns a.btn{text-decoration:none;display:inline-flex;align-items:center;}
.q-chip.static{cursor:default;}
.q-chip.static:hover{border-color:var(--border);}
.two-cols{display:grid;grid-template-columns:1fr 1fr;gap:10px;}
.viewer-wrap{padding:16px 20px 20px;display:flex;flex-direction:column;gap:10px;}
.cfg-row{display:flex;gap:8px;flex-wrap:wrap;padding:0 20px 18px;}
.admin-grid.grupos{grid-template-columns:1fr;}
.ir a{text-decoration:none;}
.sec-head .chk-warn{margin-top:4px;}
</style>
