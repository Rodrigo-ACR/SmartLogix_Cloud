<template>
    <div>
        <NavbarAdmin />
        <ConfirmModal ref="confirmRef" />

        <div class="container page">

            <!-- ENCABEZADO -->
            <div class="demo-header">
                <div>
                    <h1>🐇 Demo Evaluación Parcial 2 — RabbitMQ</h1>
                    <p class="text-muted">Desarrollo Cloud Native I — DSY1107_007D · Mensajería asíncrona entre microservicios</p>
                </div>
                <button class="btn btn-outline" @click="$router.push('/admin')">← Volver</button>
            </div>

            <!-- ESTADO GENERAL -->
            <div class="status-row">
                <span class="schip" :class="api.ok ? 'chip-ok' : api.error ? 'chip-err' : 'chip-warn'">
                    {{ api.ok ? '✅' : api.error ? '❌' : '⏳' }} rabbitadmin vía BFF
                </span>
                <span class="schip" :class="nodosActivos >= 2 ? 'chip-ok' : 'chip-warn'">
                    🧩 Cluster {{ nodosActivos }}/{{ overview.nodos.length }} nodos activos
                </span>
                <span class="schip chip-ok">📬 {{ overview.colas.length }} colas</span>
                <span class="schip chip-ok">🔀 {{ exchangesReales.length }} exchanges</span>
                <span class="schip" :class="mensajesDlq > 0 ? 'chip-warn' : 'chip-ok'">
                    ☠️ {{ mensajesDlq }} mensajes en DLQ
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
                    <strong>No se pudo leer RabbitMQ</strong>
                    <p>{{ api.error }}</p>
                </div>
                <button class="btn-retry-admin" @click="cargar()">Reintentar</button>
            </div>

            <!-- REQUISITOS EP2 -->
            <div class="demo-grid">
                <div class="demo-col">
                    <div class="req-card card">
                        <div class="req-head">
                            <span class="rn">1</span>
                            <div><h3>RabbitMQ desplegado en EC2</h3><p>Cluster de 2 nodos con Docker (mismo Erlang cookie)</p></div>
                            <span class="rbadge" :class="nodosActivos >= 2 ? 'rbadge-ok' : ''">{{ nodosActivos >= 2 ? '✓ Logrado' : '⏳' }}</span>
                        </div>
                        <div class="req-body">
                            <div class="ms-row" v-for="n in overview.nodos" :key="n.nombre">
                                <span class="ms-dot" :class="n.activo ? 'dot-ok' : 'dot-err'"></span>
                                <span class="ms-nombre">{{ n.nombre }}</span>
                                <span class="ms-port">{{ n.activo ? 'running' : 'caído' }}</span>
                            </div>
                            <div class="ir" v-if="!overview.nodos.length"><span>—</span><strong>Sin datos todavía</strong></div>
                            <div class="ir" v-if="overview.origen"><span>🌐 API</span><code>{{ overview.origen }}</code></div>
                        </div>
                    </div>

                    <div class="req-card card">
                        <div class="req-head">
                            <span class="rn">2</span>
                            <div><h3>Exchanges y colas definidos</h3><p>direct, topic y fanout, con colas durables</p></div>
                            <span class="rbadge" :class="tiposExchange.length >= 3 ? 'rbadge-ok' : ''">{{ tiposExchange.length >= 3 ? '✓ Logrado' : '⏳' }}</span>
                        </div>
                        <div class="req-body">
                            <div class="ir"><span>🔀 Tipos</span>
                                <strong>
                                    <span class="ex-badge" v-for="t in tiposExchange" :key="t" :class="'t-' + t">{{ t }}</span>
                                    <span v-if="!tiposExchange.length">—</span>
                                </strong>
                            </div>
                            <div class="ir"><span>📬 Colas</span><strong>{{ overview.colas.length }} ({{ colasDurables }} durables)</strong></div>
                            <div class="ir"><span>🔗 Bindings</span><strong>{{ bindingsReales.length }}</strong></div>
                        </div>
                    </div>

                    <div class="req-card card">
                        <div class="req-head">
                            <span class="rn">3</span>
                            <div><h3>Productores y consumidores</h3><p>Un consumidor conectado por cada cola principal</p></div>
                            <span class="rbadge" :class="todosConConsumidor ? 'rbadge-ok' : ''">{{ todosConConsumidor ? '✓ Logrado' : '⏳' }}</span>
                        </div>
                        <div class="req-body">
                            <div class="ms-row" v-for="f in flujosPrincipales" :key="f.ms.key">
                                <span class="ms-dot" :class="f.cola && f.cola.consumidores > 0 ? 'dot-ok' : 'dot-err'"></span>
                                <span class="ms-nombre">{{ f.ms.icono }} {{ f.ms.nombre }}</span>
                                <span class="ms-port">{{ f.ms.colaPrincipal }}</span>
                                <span class="ms-count">{{ f.cola ? f.cola.consumidores : 0 }} consumidor(es)</span>
                            </div>
                        </div>
                    </div>
                </div>

                <div class="demo-col">
                    <div class="req-card card">
                        <div class="req-head">
                            <span class="rn">4</span>
                            <div><h3>DLX, DLQ y ACK manual</h3><p>Los mensajes rechazados (NACK sin reencolar) van a la DLQ</p></div>
                            <span class="rbadge" :class="dlqs.length >= 4 ? 'rbadge-ok' : ''">{{ dlqs.length >= 4 ? '✓ Logrado' : '⏳' }}</span>
                        </div>
                        <div class="req-body">
                            <div class="ir"><span>☠️ DLQs</span><strong>{{ dlqs.length }} colas</strong></div>
                            <div class="ir"><span>⚙️ Con DLX</span><strong>{{ colasConDlx }} colas apuntan a un dead-letter exchange</strong></div>
                            <div class="ir"><span>✋ ACK</span><strong>Manual: ack si procesa, nack(requeue=false) si falla</strong></div>
                            <div class="test-btns">
                                <button class="btn btn-primary btn-sm" @click="probarDlq" :disabled="ocupado || !msActual">
                                    ☠️ Probar DLQ de {{ msActual ? msActual.nombre : '…' }}
                                </button>
                            </div>
                            <p class="hint" v-if="!msActual">Elige un microservicio (no «Todos») para probar su DLQ.</p>
                        </div>
                    </div>

                    <div class="req-card card">
                        <div class="req-head">
                            <span class="rn">5</span>
                            <div><h3>Seguridad: JWT en el camino a RabbitMQ</h3><p>Esta pantalla habla con el BFF, nunca con rabbitadmin</p></div>
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
                            <div class="ir"><span>🛡️ Capas</span><strong>API Gateway (JWT) → BFF (JWT) → rabbitadmin</strong></div>
                        </div>
                    </div>

                    <div class="req-card card">
                        <div class="req-head">
                            <span class="rn">6</span>
                            <div><h3>Administración y pruebas funcionales</h3><p>Crear, actualizar y eliminar por microservicio, y publicar mensajes</p></div>
                            <span class="rbadge rbadge-ok">✓ Abajo</span>
                        </div>
                        <div class="req-body">
                            <div class="ir"><span>🧭 Flujo</span><strong>Selector → Topología → Administración → Pruebas</strong></div>
                            <div class="ir"><span>🔒 Protección</span><strong>Las colas de los microservicios no se pueden borrar por error</strong></div>
                        </div>
                    </div>
                </div>
            </div>

            <!-- SELECTOR DE MICROSERVICIO -->
            <div class="card section">
                <div class="sec-head">
                    <div>
                        <h3>1 · Elegir microservicio</h3>
                        <p>Todo lo que se cree, actualice o elimine abajo queda asociado al microservicio elegido (prefijo del nombre).</p>
                    </div>
                </div>
                <div class="ms-tabs">
                    <button v-for="m in MS_LIST" :key="m.key" class="ms-tab" :class="{ active: msSel === m.key }" @click="msSel = m.key">
                        <span class="ms-ico">{{ m.icono }}</span>
                        <span class="ms-name">{{ m.nombre }}</span>
                        <span class="ms-sub">:{{ m.puerto }} · {{ conteo(m.key).colas }} colas · {{ conteo(m.key).exchanges }} exch.</span>
                    </button>
                    <button class="ms-tab" :class="{ active: msSel === 'todos' }" @click="msSel = 'todos'">
                        <span class="ms-ico">🌐</span>
                        <span class="ms-name">Todos</span>
                        <span class="ms-sub">{{ overview.colas.length }} colas · {{ exchangesReales.length }} exch.</span>
                    </button>
                </div>
                <div class="opts-row">
                    <label class="chk"><input type="checkbox" v-model="mostrarInternos" /> Mostrar exchanges internos (amq.*)</label>
                    <label class="chk chk-warn" :class="{ on: avanzado }">
                        <input type="checkbox" v-model="avanzado" /> 🔓 Modo avanzado: permitir tocar recursos del sistema
                    </label>
                </div>
                <p class="hint warn" v-if="avanzado">
                    Con el modo avanzado puedes modificar o borrar colas y exchanges que usan los microservicios. Si lo haces, ese microservicio deja de recibir sus eventos hasta que se reinicie.
                </p>
            </div>

            <!-- TOPOLOGIA -->
            <div class="card section">
                <div class="sec-head">
                    <div>
                        <h3>2 · Topología de {{ msActual ? msActual.nombre : 'todo el broker' }}</h3>
                        <p>Productor → exchange → (routing key) → cola → consumidor. Haz clic en una cola para ver sus mensajes.</p>
                    </div>
                    <span class="upd" v-if="ultimaActualizacion">actualizado {{ ultimaActualizacion }}</span>
                </div>

                <div class="flow-empty big" v-if="!exchangesVista.length && !colasVista.length">
                    No hay recursos para este microservicio todavía.
                </div>

                <div class="flow-card" v-for="ex in exchangesVista" :key="ex.nombre">
                    <div class="flow-ex">
                        <span class="flow-icon">🔀</span>
                        <strong>{{ ex.nombre }}</strong>
                        <span class="ex-badge" :class="'t-' + ex.tipo">{{ ex.tipo }}</span>
                        <span class="lock" v-if="ex.protegido" title="Lo declara un microservicio al arrancar">🔒 sistema</span>
                        <span class="lock custom" v-else-if="!ex.interno">✏️ personalizado</span>
                        <span class="dim" v-if="!ex.durable">no durable</span>
                    </div>
                    <div class="flow-rows">
                        <div class="flow-row" v-for="b in (bindingsPorExchange[ex.nombre] || [])" :key="b.origen + '|' + b.destino + '|' + b.routingKey">
                            <span class="flow-arrow">
                                <span class="line"></span>
                                <code>{{ b.routingKey || (ex.tipo === 'fanout' ? 'fanout: todas' : '(vacía)') }}</code>
                                <span class="line"></span>▶
                            </span>
                            <button class="q-chip" @click="abrirMensajes(b.destino)" :title="'Ver mensajes de ' + b.destino">
                                <span>📬 {{ b.destino }}</span>
                                <small v-if="colaPorNombre[b.destino]">
                                    listos {{ colaPorNombre[b.destino].listos }} · sin ack {{ colaPorNombre[b.destino].sinAck }} · consumidores {{ colaPorNombre[b.destino].consumidores }}
                                </small>
                            </button>
                            <span class="flow-dlx" v-if="colaPorNombre[b.destino] && colaPorNombre[b.destino].dlx">
                                ⤷ si lo rechazan → <code>{{ colaPorNombre[b.destino].dlx }}</code>
                            </span>
                        </div>
                        <div class="flow-empty" v-if="!(bindingsPorExchange[ex.nombre] || []).length">
                            Sin bindings: lo que se publique aquí se descarta.
                        </div>
                    </div>
                </div>

                <div class="flow-card orphan" v-if="colasSinBinding.length">
                    <div class="flow-ex"><span class="flow-icon">📭</span><strong>Colas sin binding</strong><span class="dim">no reciben mensajes desde ningún exchange</span></div>
                    <div class="flow-rows">
                        <div class="flow-row" v-for="q in colasSinBinding" :key="q.nombre">
                            <button class="q-chip" @click="abrirMensajes(q.nombre)">
                                <span>📬 {{ q.nombre }}</span>
                                <small>listos {{ q.listos }} · sin ack {{ q.sinAck }} · consumidores {{ q.consumidores }}</small>
                            </button>
                        </div>
                    </div>
                </div>
            </div>

            <!-- ADMINISTRACION -->
            <div class="card section">
                <div class="sec-head">
                    <div>
                        <h3>3 · Administración de {{ msActual ? msActual.nombre : 'RabbitMQ' }}</h3>
                        <p>Crear, actualizar y eliminar colas, exchanges y bindings a través del BFF → rabbitadmin.</p>
                    </div>
                </div>

                <div class="tabs">
                    <button class="tab" :class="{ active: tab === 'colas' }" @click="tab = 'colas'">📬 Colas ({{ colasVista.length }})</button>
                    <button class="tab" :class="{ active: tab === 'exchanges' }" @click="tab = 'exchanges'">🔀 Exchanges ({{ exchangesVista.length }})</button>
                    <button class="tab" :class="{ active: tab === 'bindings' }" @click="tab = 'bindings'">🔗 Bindings ({{ bindingsVista.length }})</button>
                </div>

                <div class="banner" :class="'banner-' + banner.tipo" v-if="banner.msg">
                    {{ banner.tipo === 'ok' ? '✅' : '❌' }} {{ banner.msg }}
                </div>

                <!-- COLAS -->
                <div class="admin-grid" v-if="tab === 'colas'">
                    <form class="form-card" @submit.prevent="crearCola">
                        <h4>Nueva cola</h4>
                        <div class="input-group">
                            <label>Nombre</label>
                            <div class="prefix-input">
                                <span class="prefix" v-if="prefijoCola">{{ prefijoCola }}</span>
                                <input v-model="fCola.sufijo" :placeholder="prefijoCola ? 'reportes' : 'cola.mi.nueva'" />
                            </div>
                        </div>
                        <div class="input-group">
                            <label>Dead Letter Exchange (opcional)</label>
                            <select v-model="fCola.dlx">
                                <option value="">Sin DLX</option>
                                <option v-for="e in exchangesTodos" :key="e.nombre" :value="e.nombre">{{ e.nombre }} ({{ e.tipo }})</option>
                            </select>
                        </div>
                        <label class="chk"><input type="checkbox" v-model="fCola.durable" /> Durable (sobrevive al reinicio del broker)</label>
                        <div class="input-group">
                            <label>Enlazar a un exchange (opcional)</label>
                            <select v-model="fCola.exchange">
                                <option value="">No crear binding</option>
                                <option v-for="e in exchangesVista" :key="e.nombre" :value="e.nombre">{{ e.nombre }} ({{ e.tipo }})</option>
                            </select>
                        </div>
                        <div class="input-group" v-if="fCola.exchange && tipoExchange(fCola.exchange) !== 'fanout'">
                            <label>Routing key</label>
                            <input v-model="fCola.routingKey" placeholder="pedido.creado" />
                        </div>
                        <button class="btn btn-primary" :disabled="ocupado">➕ Crear cola</button>
                        <p class="hint">Se creará como <code>{{ prefijoCola + (fCola.sufijo || '…') }}</code></p>
                    </form>

                    <div class="table-wrap">
                        <table class="tbl">
                            <thead><tr><th>Cola</th><th>Listos</th><th>Sin ack</th><th>Cons.</th><th>DLX</th><th></th></tr></thead>
                            <tbody>
                                <tr v-for="q in colasVista" :key="q.nombre">
                                    <td>
                                        <strong class="clickable" @click="abrirMensajes(q.nombre)">{{ q.nombre }}</strong>
                                        <span class="lock" v-if="q.protegido">🔒</span>
                                        <div class="dim">{{ q.durable ? 'durable' : 'no durable' }} · {{ q.estado || '—' }}</div>
                                    </td>
                                    <td>{{ q.listos }}</td>
                                    <td>{{ q.sinAck }}</td>
                                    <td>{{ q.consumidores }}</td>
                                    <td><code v-if="q.dlx">{{ q.dlx }}</code><span v-else class="dim">—</span></td>
                                    <td class="acts">
                                        <button class="ib" title="Ver mensajes" @click="abrirMensajes(q.nombre)">👁</button>
                                        <button class="ib" title="Vaciar cola" @click="purgarCola(q)" :disabled="ocupado">🧹</button>
                                        <button class="ib" title="Actualizar" @click="abrirEdicion('cola', q)" :disabled="ocupado || (q.protegido && !avanzado)">✏️</button>
                                        <button class="ib danger" title="Eliminar" @click="eliminarCola(q)" :disabled="ocupado || (q.protegido && !avanzado)">🗑️</button>
                                    </td>
                                </tr>
                                <tr v-if="!colasVista.length"><td colspan="6" class="empty">Sin colas para este microservicio.</td></tr>
                            </tbody>
                        </table>
                    </div>
                </div>

                <!-- EXCHANGES -->
                <div class="admin-grid" v-if="tab === 'exchanges'">
                    <form class="form-card" @submit.prevent="crearExchange">
                        <h4>Nuevo exchange</h4>
                        <div class="input-group">
                            <label>Nombre</label>
                            <div class="prefix-input">
                                <span class="prefix" v-if="prefijoExchange">{{ prefijoExchange }}</span>
                                <input v-model="fExchange.sufijo" :placeholder="prefijoExchange ? 'auditoria' : 'mi.exchange'" />
                            </div>
                        </div>
                        <div class="input-group">
                            <label>Tipo</label>
                            <select v-model="fExchange.tipo">
                                <option value="direct">direct — una routing key exacta</option>
                                <option value="topic">topic — patrones con * y #</option>
                                <option value="fanout">fanout — a todas las colas</option>
                                <option value="headers">headers — por cabeceras</option>
                            </select>
                        </div>
                        <label class="chk"><input type="checkbox" v-model="fExchange.durable" /> Durable</label>
                        <button class="btn btn-primary" :disabled="ocupado">➕ Crear exchange</button>
                        <p class="hint">Se creará como <code>{{ prefijoExchange + (fExchange.sufijo || '…') }}</code></p>
                    </form>

                    <div class="table-wrap">
                        <table class="tbl">
                            <thead><tr><th>Exchange</th><th>Tipo</th><th>Durable</th><th></th></tr></thead>
                            <tbody>
                                <tr v-for="e in exchangesVista" :key="e.nombre">
                                    <td>
                                        <strong>{{ e.nombre }}</strong>
                                        <span class="lock" v-if="e.protegido">🔒</span>
                                        <div class="dim" v-if="compartidoCon(e.nombre)">compartido: {{ compartidoCon(e.nombre) }}</div>
                                    </td>
                                    <td><span class="ex-badge" :class="'t-' + e.tipo">{{ e.tipo }}</span></td>
                                    <td>{{ e.durable ? 'sí' : 'no' }}</td>
                                    <td class="acts">
                                        <button class="ib" title="Actualizar" @click="abrirEdicion('exchange', e)" :disabled="ocupado || e.interno || (e.protegido && !avanzado)">✏️</button>
                                        <button class="ib danger" title="Eliminar" @click="eliminarExchange(e)" :disabled="ocupado || e.interno || (e.protegido && !avanzado)">🗑️</button>
                                    </td>
                                </tr>
                                <tr v-if="!exchangesVista.length"><td colspan="4" class="empty">Sin exchanges para este microservicio.</td></tr>
                            </tbody>
                        </table>
                    </div>
                </div>

                <!-- BINDINGS -->
                <div class="admin-grid" v-if="tab === 'bindings'">
                    <form class="form-card" @submit.prevent="crearBinding">
                        <h4>Nuevo binding</h4>
                        <div class="input-group">
                            <label>Exchange (origen)</label>
                            <select v-model="fBinding.exchange">
                                <option value="" disabled>Elegir exchange…</option>
                                <option v-for="e in exchangesVista" :key="e.nombre" :value="e.nombre">{{ e.nombre }} ({{ e.tipo }})</option>
                            </select>
                        </div>
                        <div class="input-group">
                            <label>Cola (destino)</label>
                            <select v-model="fBinding.cola">
                                <option value="" disabled>Elegir cola…</option>
                                <option v-for="q in colasVista" :key="q.nombre" :value="q.nombre">{{ q.nombre }}</option>
                            </select>
                        </div>
                        <div class="input-group" v-if="tipoExchange(fBinding.exchange) !== 'fanout'">
                            <label>Routing key</label>
                            <input v-model="fBinding.routingKey" :placeholder="tipoExchange(fBinding.exchange) === 'topic' ? 'pedidos.#' : 'pedido.creado'" />
                        </div>
                        <button class="btn btn-primary" :disabled="ocupado || !fBinding.exchange || !fBinding.cola">➕ Crear binding</button>
                        <p class="hint">En un <b>fanout</b> no hace falta routing key; en un <b>topic</b> acepta <code>*</code> y <code>#</code>.</p>
                    </form>

                    <div class="table-wrap">
                        <table class="tbl">
                            <thead><tr><th>Exchange</th><th>Routing key</th><th>Cola</th><th></th></tr></thead>
                            <tbody>
                                <tr v-for="b in bindingsVista" :key="b.origen + '|' + b.destino + '|' + b.routingKey">
                                    <td><strong>{{ b.origen }}</strong> <span class="lock" v-if="b.protegido">🔒</span></td>
                                    <td><code>{{ b.routingKey || '—' }}</code></td>
                                    <td>{{ b.destino }}</td>
                                    <td class="acts">
                                        <button class="ib" title="Cambiar routing key" @click="abrirEdicion('binding', b)" :disabled="ocupado || b.tipoDestino !== 'queue' || (b.protegido && !avanzado)">✏️</button>
                                        <button class="ib danger" title="Eliminar" @click="eliminarBinding(b)" :disabled="ocupado || b.tipoDestino !== 'queue' || (b.protegido && !avanzado)">🗑️</button>
                                    </td>
                                </tr>
                                <tr v-if="!bindingsVista.length"><td colspan="4" class="empty">Sin bindings para este microservicio.</td></tr>
                            </tbody>
                        </table>
                    </div>
                </div>
            </div>

            <!-- PRUEBAS FUNCIONALES -->
            <div class="card section">
                <div class="sec-head">
                    <div>
                        <h3>4 · Pruebas funcionales</h3>
                        <p>Publica un mensaje en un exchange y mira cómo llega a la cola, o cómo termina en la DLQ si el consumidor lo rechaza.</p>
                    </div>
                </div>

                <div class="test-grid">
                    <form class="form-card" @submit.prevent="publicar">
                        <h4>Publicar mensaje</h4>
                        <div class="input-group">
                            <label>Exchange</label>
                            <select v-model="pub.exchange">
                                <option value="">(exchange por defecto)</option>
                                <option v-for="e in exchangesTodos" :key="e.nombre" :value="e.nombre">{{ e.nombre }} ({{ e.tipo }})</option>
                            </select>
                        </div>
                        <div class="input-group">
                            <label>Routing key</label>
                            <input v-model="pub.routingKey" placeholder="pedido.creado" />
                        </div>
                        <div class="input-group">
                            <label>Content-Type</label>
                            <select v-model="pub.contentType">
                                <option value="application/json">application/json</option>
                                <option value="text/plain">text/plain (los consumidores lo rechazan)</option>
                            </select>
                        </div>
                        <div class="input-group">
                            <label>Mensaje</label>
                            <textarea v-model="pub.mensaje" rows="5" spellcheck="false"></textarea>
                        </div>
                        <div class="test-btns" v-if="msActual">
                            <button type="button" class="btn btn-outline btn-sm" @click="cargarPreset('valido')">✅ Cargar evento válido</button>
                            <button type="button" class="btn btn-outline btn-sm" @click="cargarPreset('invalido')">☠️ Cargar evento inválido</button>
                        </div>
                        <button class="btn btn-primary" :disabled="ocupado || !pub.mensaje.trim()">📤 Publicar</button>
                    </form>

                    <div class="form-card viewer">
                        <h4>Mensajes de una cola</h4>
                        <div class="viewer-bar">
                            <select v-model="visor.cola">
                                <option value="" disabled>Elegir cola…</option>
                                <option v-for="q in overview.colas" :key="q.nombre" :value="q.nombre">{{ q.nombre }} ({{ q.total }})</option>
                            </select>
                            <button class="btn btn-outline btn-sm" @click="abrirMensajes(visor.cola)" :disabled="!visor.cola || visor.cargando">👁 Ver</button>
                        </div>
                        <p class="hint">Se miran sin consumirlos: vuelven a la cola. Si la cola tiene un consumidor conectado, normalmente la verás vacía porque ya los procesó.</p>
                        <p class="hint warn" v-if="visor.error">{{ visor.error }}</p>

                        <div class="msg" v-for="(m, i) in visor.mensajes" :key="i">
                            <div class="msg-head">
                                <span>#{{ i + 1 }}</span>
                                <span>exchange <code>{{ m.exchange || '(default)' }}</code></span>
                                <span>key <code>{{ m.routing_key || '—' }}</code></span>
                                <span v-if="m.redelivered" class="dim">reentregado</span>
                            </div>
                            <div class="msg-death" v-if="muerte(m)">☠️ {{ muerte(m) }}</div>
                            <pre>{{ bonito(m.payload) }}</pre>
                        </div>
                        <div class="flow-empty" v-if="visor.consultado && !visor.mensajes.length && !visor.error">
                            La cola <code>{{ visor.cola }}</code> no tiene mensajes en este momento.
                        </div>
                    </div>
                </div>
            </div>

            <!-- LOGGER -->
            <div class="card logger-card">
                <div class="logger-top">
                    <h3>🖥️ System Logger — Actividad en tiempo real</h3>
                    <button class="btn-clear" @click="logs = []">Limpiar</button>
                </div>
                <div class="terminal">
                    <div class="log-line" v-for="(l, i) in logs" :key="i" :class="'log-' + l.type">
                        <span class="lt">{{ l.time }}</span>
                        <span class="li">{{ l.icon }}</span>
                        <span class="lm">{{ l.msg }}</span>
                    </div>
                    <div class="log-empty" v-if="logs.length === 0">Sin actividad.</div>
                </div>
            </div>

        </div>

        <!-- MODAL DE EDICION -->
        <div class="overlay" v-if="edit.visible" @click.self="cerrarEdicion">
            <div class="modal">
                <h3>
                    {{ edit.tipo === 'cola' ? '✏️ Actualizar cola' : edit.tipo === 'exchange' ? '✏️ Actualizar exchange' : '✏️ Cambiar routing key' }}
                </h3>
                <p class="modal-name">{{ edit.titulo }}</p>

                <template v-if="edit.tipo === 'cola'">
                    <div class="input-group">
                        <label>Dead Letter Exchange</label>
                        <select v-model="edit.form.dlx">
                            <option value="">Sin DLX</option>
                            <option v-for="e in exchangesTodos" :key="e.nombre" :value="e.nombre">{{ e.nombre }} ({{ e.tipo }})</option>
                        </select>
                    </div>
                    <label class="chk"><input type="checkbox" v-model="edit.form.durable" /> Durable</label>
                    <p class="hint warn">RabbitMQ no permite cambiar estos valores en una cola existente. Se elimina y se vuelve a crear conservando sus bindings; los mensajes que tenga se pierden.</p>
                </template>

                <template v-if="edit.tipo === 'exchange'">
                    <div class="input-group">
                        <label>Tipo</label>
                        <select v-model="edit.form.tipo">
                            <option value="direct">direct</option>
                            <option value="topic">topic</option>
                            <option value="fanout">fanout</option>
                            <option value="headers">headers</option>
                        </select>
                    </div>
                    <label class="chk"><input type="checkbox" v-model="edit.form.durable" /> Durable</label>
                    <p class="hint warn">El tipo de un exchange no se puede cambiar en caliente. Se elimina y se vuelve a crear restaurando sus bindings hacia colas.</p>
                </template>

                <template v-if="edit.tipo === 'binding'">
                    <div class="input-group">
                        <label>Routing key actual</label>
                        <input :value="edit.objetivo.routingKey || '(vacía)'" disabled />
                    </div>
                    <div class="input-group">
                        <label>Routing key nueva</label>
                        <input v-model="edit.form.routingKey" placeholder="pedido.actualizado" />
                    </div>
                </template>

                <div class="modal-actions">
                    <button class="btn btn-outline btn-sm" @click="cerrarEdicion">Cancelar</button>
                    <button class="btn btn-primary btn-sm" @click="guardarEdicion" :disabled="ocupado">Guardar cambios</button>
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
    rabbitOverview,
    rabbitCrearCola, rabbitActualizarCola, rabbitEliminarCola, rabbitPurgarCola, rabbitVerMensajes,
    rabbitCrearExchange, rabbitActualizarExchange, rabbitEliminarExchange,
    rabbitCrearBinding, rabbitActualizarBinding, rabbitEliminarBinding,
    rabbitPublicar
} from '../services/api.js';

// ---------------------------------------------------------------------
// Microservicios y su "dueño" en RabbitMQ (se reconoce por el prefijo del nombre)
// ---------------------------------------------------------------------
const MS_LIST = [
    {
        key: 'pedidos', nombre: 'Pedidos', icono: '🛒', puerto: 8092,
        colaPrefijo: 'cola.pedidos.', exPrefijo: 'pedidos.',
        colaPrincipal: 'cola.pedidos.creado', dlq: 'cola.pedidos.dlq',
        exchange: 'pedidos.direct.exchange', routingKey: 'pedido.creado',
        valido: '{\n  "pedidoId": 9001,\n  "cliente": "demo@smartlogix.cl"\n}',
        invalido: '{\n  "comentario": "falta pedidoId y cliente"\n}'
    },
    {
        key: 'envios', nombre: 'Envíos', icono: '🚚', puerto: 8093,
        colaPrefijo: 'cola.envios.', exPrefijo: 'envios.',
        colaPrincipal: 'cola.envios.notificacion', dlq: 'cola.envios.dlq',
        exchange: 'notificaciones.topic.exchange', routingKey: 'envios.actualizado',
        valido: '{\n  "envioId": 9001,\n  "estado": "EN_CAMINO"\n}',
        invalido: '{\n  "comentario": "falta envioId y estado"\n}'
    },
    {
        key: 'usuarios', nombre: 'Usuarios', icono: '👥', puerto: 8094,
        colaPrefijo: 'cola.usuarios.', exPrefijo: 'usuarios.',
        colaPrincipal: 'cola.usuarios.bienvenida', dlq: 'cola.usuarios.dlq',
        exchange: 'notificaciones.topic.exchange', routingKey: 'usuarios.bienvenida',
        valido: '{\n  "nombre": "Usuario Demo",\n  "correo": "demo@smartlogix.cl"\n}',
        invalido: '{\n  "comentario": "falta nombre y correo"\n}'
    },
    {
        key: 'inventario', nombre: 'Inventario', icono: '📦', puerto: 8091,
        colaPrefijo: 'cola.inventario.', exPrefijo: 'inventario.',
        colaPrincipal: 'cola.inventario.stockbajo', dlq: 'cola.inventario.dlq',
        exchange: 'inventario.fanout.exchange', routingKey: '',
        valido: '{\n  "productoId": 9001,\n  "nombreProducto": "Producto demo",\n  "stockActual": 2\n}',
        invalido: '{\n  "comentario": "falta productoId y nombreProducto"\n}'
    }
];

// Exchanges que usan varios microservicios
const COMPARTIDOS = { 'notificaciones.topic.exchange': ['envios', 'usuarios'] };

const porClave = Object.fromEntries(MS_LIST.map(m => [m.key, m]));

function colaDe(clave, q) {
    if (clave === 'todos') return true;
    return q.nombre.startsWith(porClave[clave].colaPrefijo);
}
function exchangeDe(clave, e) {
    if (clave === 'todos') return true;
    return e.nombre.startsWith(porClave[clave].exPrefijo) || (COMPARTIDOS[e.nombre] || []).includes(clave);
}
function bindingDe(clave, b) {
    if (clave === 'todos') return true;
    const m = porClave[clave];
    const colaMia = b.tipoDestino === 'queue' && b.destino.startsWith(m.colaPrefijo);
    return colaMia || b.origen.startsWith(m.exPrefijo);
}

// ---------------------------------------------------------------------
// Estado
// ---------------------------------------------------------------------
const confirmRef = ref(null);
const confirmar = (opts) => confirmRef.value.abrir(opts);

const overview = reactive({ origen: '', nodos: [], colas: [], exchanges: [], bindings: [] });
const api = reactive({ ok: false, error: '' });
const cargando = ref(false);
const ocupado = ref(false);
const auto = ref(true);
const ultimaActualizacion = ref('');

const msSel = ref('pedidos');
const tab = ref('colas');
const mostrarInternos = ref(false);
const avanzado = ref(false);

const logs = ref([]);
const banner = reactive({ tipo: 'ok', msg: '' });
const seguridad = reactive({ sinToken: null, conToken: null });

const fCola = reactive({ sufijo: '', dlx: '', durable: true, exchange: '', routingKey: '' });
const fExchange = reactive({ sufijo: '', tipo: 'direct', durable: true });
const fBinding = reactive({ exchange: '', cola: '', routingKey: '' });
const pub = reactive({ exchange: '', routingKey: '', contentType: 'application/json', mensaje: '{\n  \n}' });
const visor = reactive({ cola: '', mensajes: [], cargando: false, error: '', consultado: false });
const edit = reactive({ visible: false, tipo: '', titulo: '', objetivo: null, form: {} });

let temporizador = null;
let bannerTimer = null;
let ultimoErrorLog = '';

// ---------------------------------------------------------------------
// Datos derivados
// ---------------------------------------------------------------------
const msActual = computed(() => porClave[msSel.value] || null);

const exchangesReales = computed(() => overview.exchanges.filter(e => !e.interno));
const exchangesTodos = computed(() => overview.exchanges.filter(e => mostrarInternos.value || !e.interno));
const bindingsReales = computed(() => overview.bindings.filter(b => !b.origen.startsWith('amq.')));

const colasVista = computed(() => overview.colas.filter(q => colaDe(msSel.value, q)));
const exchangesVista = computed(() => exchangesTodos.value.filter(e => exchangeDe(msSel.value, e)));
const bindingsVista = computed(() =>
    overview.bindings.filter(b => bindingDe(msSel.value, b) && (mostrarInternos.value || !b.origen.startsWith('amq.'))));

const colaPorNombre = computed(() => Object.fromEntries(overview.colas.map(q => [q.nombre, q])));

const bindingsPorExchange = computed(() => {
    const mapa = {};
    for (const b of bindingsVista.value) {
        if (!mapa[b.origen]) mapa[b.origen] = [];
        mapa[b.origen].push(b);
    }
    return mapa;
});

const colasSinBinding = computed(() => {
    const conBinding = new Set(overview.bindings.filter(b => b.tipoDestino === 'queue').map(b => b.destino));
    return colasVista.value.filter(q => !conBinding.has(q.nombre));
});

const nodosActivos = computed(() => overview.nodos.filter(n => n.activo).length);
const tiposExchange = computed(() => [...new Set(exchangesReales.value.map(e => e.tipo))]);
const colasDurables = computed(() => overview.colas.filter(q => q.durable).length);
const dlqs = computed(() => overview.colas.filter(q => q.nombre.endsWith('.dlq')));
const mensajesDlq = computed(() => dlqs.value.reduce((suma, q) => suma + q.listos + q.sinAck, 0));
const colasConDlx = computed(() => overview.colas.filter(q => q.dlx).length);
const flujosPrincipales = computed(() => MS_LIST.map(m => ({ ms: m, cola: colaPorNombre.value[m.colaPrincipal] })));
const todosConConsumidor = computed(() =>
    flujosPrincipales.value.length > 0 && flujosPrincipales.value.every(f => f.cola && f.cola.consumidores > 0));

const prefijoCola = computed(() => (msActual.value ? msActual.value.colaPrefijo : ''));
const prefijoExchange = computed(() => (msActual.value ? msActual.value.exPrefijo : ''));

function conteo(clave) {
    return {
        colas: overview.colas.filter(q => colaDe(clave, q)).length,
        exchanges: exchangesReales.value.filter(e => exchangeDe(clave, e)).length
    };
}
function tipoExchange(nombre) {
    const e = overview.exchanges.find(x => x.nombre === nombre);
    return e ? e.tipo : '';
}
function compartidoCon(nombre) {
    const lista = COMPARTIDOS[nombre];
    return lista ? lista.map(k => porClave[k].nombre).join(' y ') : '';
}

// ---------------------------------------------------------------------
// Utilidades
// ---------------------------------------------------------------------
const esperar = (ms) => new Promise(r => setTimeout(r, ms));

function addLog(type, icon, msg) {
    const time = new Date().toLocaleTimeString('es-CL', { hour12: false });
    logs.value.unshift({ type, icon, msg, time });
    if (logs.value.length > 60) logs.value.pop();
}

function avisar(tipo, msg) {
    banner.tipo = tipo;
    banner.msg = msg;
    clearTimeout(bannerTimer);
    bannerTimer = setTimeout(() => { banner.msg = ''; }, 7000);
}

function bonito(texto) {
    try { return JSON.stringify(JSON.parse(texto), null, 2); } catch { return texto; }
}

function muerte(m) {
    const h = m && m.properties && m.properties.headers;
    const xd = h && h['x-death'];
    if (!Array.isArray(xd) || !xd.length) return '';
    const d = xd[0];
    return `Llegó por dead-letter · motivo: ${d.reason} · cola de origen: ${d.queue} · veces: ${d.count}`;
}

const nombreValido = (texto) => /^[a-zA-Z0-9_.\-]+$/.test(texto);

// ---------------------------------------------------------------------
// Carga de la topologia
// ---------------------------------------------------------------------
async function cargar(silencioso = false, fresco = false) {
    if (cargando.value) {
        if (!fresco) return;
        // hay una lectura en curso (quizá anterior al cambio): se espera y se vuelve a leer
        while (cargando.value) await esperar(100);
    }
    cargando.value = true;
    try {
        const d = await rabbitOverview();
        overview.origen = d.origen || '';
        overview.nodos = d.nodos || [];
        overview.colas = d.colas || [];
        overview.exchanges = d.exchanges || [];
        overview.bindings = d.bindings || [];
        if (!api.ok && !silencioso) {
            addLog('success', '📡', `Topología leída vía BFF: ${overview.colas.length} colas, ${exchangesReales.value.length} exchanges, ${bindingsReales.value.length} bindings`);
        }
        api.ok = true;
        api.error = '';
        ultimoErrorLog = '';
        ultimaActualizacion.value = new Date().toLocaleTimeString('es-CL', { hour12: false });
    } catch (e) {
        api.ok = false;
        api.error = e.message;
        if (e.message !== ultimoErrorLog) {
            addLog('error', '❌', `No se pudo leer RabbitMQ: ${e.message}`);
            ultimoErrorLog = e.message;
        }
    } finally {
        cargando.value = false;
    }
}

function programarAuto() {
    clearInterval(temporizador);
    temporizador = null;
    if (auto.value) {
        temporizador = setInterval(() => { if (!ocupado.value) cargar(true); }, 5000);
    }
}

// Ejecuta una operacion contra rabbitadmin, la registra en el logger y refresca la vista
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
// Colas
// ---------------------------------------------------------------------
async function crearCola() {
    const sufijo = fCola.sufijo.trim();
    if (!sufijo) return avisar('err', 'Escribe el nombre de la cola.');
    const nombre = prefijoCola.value + sufijo;
    if (!nombreValido(nombre)) return avisar('err', 'El nombre solo puede tener letras, números, puntos, guiones y guiones bajos.');

    const r = await ejecutar(`POST cola ${nombre}`, () =>
        rabbitCrearCola({ nombre, durable: fCola.durable, deadLetterExchange: fCola.dlx || null }), !fCola.exchange);
    if (!r.ok) return;

    if (fCola.exchange) {
        const rk = tipoExchange(fCola.exchange) === 'fanout' ? '' : fCola.routingKey.trim();
        const b = await ejecutar(`POST binding ${fCola.exchange} → ${nombre} [${rk || 'sin key'}]`, () =>
            rabbitCrearBinding({ exchange: fCola.exchange, cola: nombre, routingKey: rk }));
        if (!b.ok) return avisar('err', `La cola se creó, pero el binding falló: ${b.error}`);
    }
    avisar('ok', `Cola «${nombre}» creada.`);
    fCola.sufijo = '';
    fCola.exchange = '';
    fCola.routingKey = '';
}

async function eliminarCola(q) {
    const force = q.protegido && avanzado.value;
    const ok = await confirmar({
        titulo: 'Eliminar cola',
        mensaje: `Se eliminará «${q.nombre}» con sus ${q.total} mensajes.` + (force ? ' ATENCIÓN: esta cola la usa un microservicio.' : ''),
        textoConfirmar: 'Eliminar'
    });
    if (!ok) return;
    const r = await ejecutar(`DELETE cola ${q.nombre}`, () => rabbitEliminarCola(q.nombre, force));
    if (r.ok) avisar('ok', `Cola «${q.nombre}» eliminada.`);
}

async function purgarCola(q) {
    const ok = await confirmar({
        titulo: 'Vaciar cola',
        mensaje: `Se borrarán los ${q.total} mensajes de «${q.nombre}». La cola y sus bindings se mantienen.`,
        icono: '🧹', textoConfirmar: 'Vaciar'
    });
    if (!ok) return;
    const r = await ejecutar(`DELETE mensajes de ${q.nombre}`, () => rabbitPurgarCola(q.nombre));
    if (r.ok) {
        avisar('ok', `Cola «${q.nombre}» vaciada.`);
        if (visor.cola === q.nombre) visor.mensajes = [];
    }
}

// ---------------------------------------------------------------------
// Exchanges
// ---------------------------------------------------------------------
async function crearExchange() {
    const sufijo = fExchange.sufijo.trim();
    if (!sufijo) return avisar('err', 'Escribe el nombre del exchange.');
    const nombre = prefijoExchange.value + sufijo;
    if (!nombreValido(nombre)) return avisar('err', 'El nombre solo puede tener letras, números, puntos, guiones y guiones bajos.');

    const r = await ejecutar(`POST exchange ${nombre} (${fExchange.tipo})`, () =>
        rabbitCrearExchange({ nombre, tipo: fExchange.tipo, durable: fExchange.durable }));
    if (r.ok) {
        avisar('ok', `Exchange «${nombre}» creado.`);
        fExchange.sufijo = '';
    }
}

async function eliminarExchange(e) {
    const force = e.protegido && avanzado.value;
    const ok = await confirmar({
        titulo: 'Eliminar exchange',
        mensaje: `Se eliminará «${e.nombre}» y todos sus bindings.` + (force ? ' ATENCIÓN: lo usa un microservicio.' : ''),
        textoConfirmar: 'Eliminar'
    });
    if (!ok) return;
    const r = await ejecutar(`DELETE exchange ${e.nombre}`, () => rabbitEliminarExchange(e.nombre, force));
    if (r.ok) avisar('ok', `Exchange «${e.nombre}» eliminado.`);
}

// ---------------------------------------------------------------------
// Bindings
// ---------------------------------------------------------------------
async function crearBinding() {
    const rk = tipoExchange(fBinding.exchange) === 'fanout' ? '' : fBinding.routingKey.trim();
    const r = await ejecutar(`POST binding ${fBinding.exchange} → ${fBinding.cola} [${rk || 'sin key'}]`, () =>
        rabbitCrearBinding({ exchange: fBinding.exchange, cola: fBinding.cola, routingKey: rk }));
    if (r.ok) {
        avisar('ok', 'Binding creado.');
        fBinding.routingKey = '';
    }
}

async function eliminarBinding(b) {
    const force = b.protegido && avanzado.value;
    const ok = await confirmar({
        titulo: 'Eliminar binding',
        mensaje: `Se quitará el enlace «${b.origen}» → «${b.destino}» (${b.routingKey || 'sin routing key'}). La cola dejará de recibir esos mensajes.`,
        textoConfirmar: 'Eliminar'
    });
    if (!ok) return;
    const r = await ejecutar(`DELETE binding ${b.origen} → ${b.destino}`, () =>
        rabbitEliminarBinding({ exchange: b.origen, cola: b.destino, routingKey: b.routingKey }, force));
    if (r.ok) avisar('ok', 'Binding eliminado.');
}

// ---------------------------------------------------------------------
// Edicion (cola, exchange y binding)
// ---------------------------------------------------------------------
function abrirEdicion(tipo, objetivo) {
    edit.tipo = tipo;
    edit.objetivo = objetivo;
    if (tipo === 'cola') {
        edit.titulo = objetivo.nombre;
        edit.form = { dlx: objetivo.dlx || '', durable: objetivo.durable };
    } else if (tipo === 'exchange') {
        edit.titulo = objetivo.nombre;
        edit.form = { tipo: objetivo.tipo, durable: objetivo.durable };
    } else {
        edit.titulo = `${objetivo.origen} → ${objetivo.destino}`;
        edit.form = { routingKey: objetivo.routingKey };
    }
    edit.visible = true;
}

function cerrarEdicion() {
    edit.visible = false;
}

async function guardarEdicion() {
    const o = edit.objetivo;
    const force = !!o.protegido && avanzado.value;

    if (edit.tipo === 'cola') {
        const ok = await confirmar({
            titulo: 'Actualizar cola',
            mensaje: `«${o.nombre}» se eliminará y se creará de nuevo con los valores nuevos. Se pierden sus ${o.total} mensajes; los bindings se restauran.`,
            icono: '♻️', tipo: 'primary', textoConfirmar: 'Actualizar'
        });
        if (!ok) return;
        cerrarEdicion();
        const r = await ejecutar(`PUT cola ${o.nombre}`, () =>
            rabbitActualizarCola(o.nombre, { durable: edit.form.durable, deadLetterExchange: edit.form.dlx || null }, force));
        if (r.ok) avisar('ok', `Cola «${o.nombre}» actualizada (${r.data.bindingsRestaurados} bindings restaurados).`);
    } else if (edit.tipo === 'exchange') {
        const ok = await confirmar({
            titulo: 'Actualizar exchange',
            mensaje: `«${o.nombre}» se eliminará y se creará de nuevo como ${edit.form.tipo}. Sus bindings hacia colas se restauran.`,
            icono: '♻️', tipo: 'primary', textoConfirmar: 'Actualizar'
        });
        if (!ok) return;
        cerrarEdicion();
        const r = await ejecutar(`PUT exchange ${o.nombre}`, () =>
            rabbitActualizarExchange(o.nombre, { tipo: edit.form.tipo, durable: edit.form.durable }, force));
        if (r.ok) avisar('ok', `Exchange «${o.nombre}» actualizado (${r.data.bindingsRestaurados} bindings restaurados).`);
    } else {
        const nueva = (edit.form.routingKey || '').trim();
        cerrarEdicion();
        const r = await ejecutar(`PUT binding ${o.origen} → ${o.destino} [${o.routingKey} → ${nueva}]`, () =>
            rabbitActualizarBinding({ exchange: o.origen, cola: o.destino, routingKey: o.routingKey, nuevaRoutingKey: nueva }, force));
        if (r.ok) avisar('ok', 'Routing key actualizada.');
    }
}

// ---------------------------------------------------------------------
// Pruebas funcionales
// ---------------------------------------------------------------------
function cargarPreset(tipo) {
    const m = msActual.value;
    if (!m) return;
    pub.exchange = m.exchange;
    pub.routingKey = m.routingKey;
    pub.contentType = 'application/json';
    pub.mensaje = tipo === 'valido' ? m.valido : m.invalido;
}

async function publicar() {
    const r = await ejecutar(`POST publish ${pub.exchange || '(default)'} [${pub.routingKey || 'sin key'}]`, () =>
        rabbitPublicar({
            exchange: pub.exchange, routingKey: pub.routingKey,
            mensaje: pub.mensaje, contentType: pub.contentType
        }));
    if (r.ok) avisar('ok', 'Mensaje publicado. Mira los contadores de la topología.');
}

async function probarDlq() {
    const m = msActual.value;
    if (!m) return;
    const r = await ejecutar(`Evento inválido → ${m.exchange} [${m.routingKey || 'fanout'}]`, () =>
        rabbitPublicar({ exchange: m.exchange, routingKey: m.routingKey, mensaje: m.invalido, contentType: 'application/json' }), false);
    if (!r.ok) return;
    addLog('info', '⏳', `Esperando que ${m.colaPrincipal} rechace el mensaje (NACK) y llegue a ${m.dlq}...`);
    await esperar(2000);
    await cargar(true, true);
    await abrirMensajes(m.dlq);
    const dlq = colaPorNombre.value[m.dlq];
    if (dlq && dlq.total > 0) {
        addLog('success', '☠️', `DLQ ${m.dlq} tiene ${dlq.total} mensaje(s): el NACK sin reencolar funciona`);
        avisar('ok', `El mensaje inválido terminó en ${m.dlq}.`);
    } else {
        addLog('warn', '⚠️', `${m.dlq} sigue vacía. Revisa que el consumidor de ${m.colaPrincipal} esté activo.`);
        avisar('err', `No se vio el mensaje en ${m.dlq}. Actualiza en unos segundos o revisa los logs del microservicio.`);
    }
}

async function abrirMensajes(cola) {
    if (!cola) return;
    visor.cola = cola;
    visor.cargando = true;
    visor.error = '';
    try {
        visor.mensajes = await rabbitVerMensajes(cola, 10);
        visor.consultado = true;
        addLog('info', '👁', `Mensajes de ${cola}: ${visor.mensajes.length}`);
    } catch (e) {
        visor.mensajes = [];
        visor.error = e.message;
        addLog('error', '❌', `Ver mensajes de ${cola} → ${e.message}`);
    } finally {
        visor.cargando = false;
    }
}

// ---------------------------------------------------------------------
// Seguridad (JWT)
// ---------------------------------------------------------------------
async function testSinToken() {
    ocupado.value = true;
    addLog('info', '🧪', 'Test SIN token → GET /api/rabbit/overview');
    try {
        const res = await fetch('/api/rabbit/overview', { headers: { 'ngrok-skip-browser-warning': 'true' } });
        if (res.status === 401 || res.status === 403) {
            seguridad.sinToken = { ok: true, msg: `${res.status} rechazado ✓` };
            addLog('success', '🔐', `Sin token → ${res.status}: el BFF protege RabbitMQ`);
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
    addLog('info', '🧪', 'Test CON token → GET /api/rabbit/overview');
    try {
        const d = await rabbitOverview();
        seguridad.conToken = { ok: true, msg: `200 OK · ${d.colas.length} colas ✓` };
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

watch(msSel, () => {
    // Al cambiar de microservicio se limpian los formularios y se sugiere su DLX
    const m = msActual.value;
    fCola.sufijo = '';
    fCola.exchange = '';
    fCola.routingKey = '';
    fCola.dlx = m && overview.exchanges.some(e => e.nombre === m.exPrefijo + 'dlx') ? m.exPrefijo + 'dlx' : '';
    fExchange.sufijo = '';
    fBinding.exchange = '';
    fBinding.cola = '';
    fBinding.routingKey = '';
    if (m) cargarPreset('valido');
});

onMounted(async () => {
    addLog('info', '🚀', 'Demo EP2 iniciada — consultando RabbitMQ a través del BFF...');
    await cargar();
    const m = msActual.value;
    if (m) {
        cargarPreset('valido');
        if (overview.exchanges.some(e => e.nombre === m.exPrefijo + 'dlx')) fCola.dlx = m.exPrefijo + 'dlx';
        visor.cola = m.dlq;
    }
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
</style>
