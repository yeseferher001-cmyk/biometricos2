# Proyecto Atleta de Alto Rendimiento - Examen Parcial 2

## Especificaciones del Sistema
Este proyecto es una herramienta móvil diseñada para atletas de alto rendimiento que permite registrar avances mediante voz y biometría, persistiendo datos en la nube (MongoDB Atlas).

### Requerimientos Implementados
*   **RF01 (Autenticación Biométrica)**: El acceso está bloqueado por huella digital o rostro.
*   **RF02 (Captura por Voz)**: Interfaz de grabación que transcribe audio a texto en tiempo real.
*   **RF03 (Métricas)**: Algoritmo que extrae automáticamente Kilómetros y Minutos del dictado.
*   **RF04 (Persistencia)**: Conexión mediante API REST (Node.js) para insertar documentos en MongoDB Atlas.
*   **RF05 (Gráficas)**: Visualización estadística del rendimiento semanal usando KoalaPlot.
*   **RNF01 (Seguridad)**: Las credenciales de la base de datos están protegidas en el Backend (Node.js), no en la App.
*   **RNF02 (Usabilidad)**: Botones grandes y feedback visual para uso con manos fatigadas.
*   **RNF03 (Tolerancia a Fallos)**: Manejo de errores biométricos mediante Toasts informativos.
*   **RNF04 (Disponibilidad)**: Alerta de falta de internet antes de intentar subir datos.

## Guía para la API (Backend Node.js)
Para conectar este proyecto con MongoDB, debes usar un servidor Node.js. Ejemplo de script:

```javascript
const express = require('express');
const { MongoClient } = require('mongodb');
const app = express();
app.use(express.json());

const uri = "tu-conexion-mongodb-atlas";
const client = new MongoClient(uri);

app.post('/trainings', async (req, res) => {
    const db = client.db("atletaDB");
    const result = await db.collection("trainings").insertOne({
        ...req.body,
        timestamp: new Date()
    });
    res.status(201).send(result);
});

app.get('/trainings/:username', async (req, res) => {
    const db = client.db("atletaDB");
    const docs = await db.collection("trainings")
        .find({ username: req.params.username })
        .sort({ timestamp: -1 }).toArray();
    res.send(docs);
});

app.listen(3000, () => console.log("API de Atleta corriendo en puerto 3000"));
```
