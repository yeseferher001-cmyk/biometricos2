const express = require('express');
const mongoose = require('mongoose');
const cors = require('cors');

const app = express();
app.use(express.json());
app.use(cors());

// Conexión a MongoDB Atlas
const mongoUri = "mongodb+srv://yeseferher001_db_user:sK46NXlaDnwmYP5o@appcluster.5kb6k4k.mongodb.net/biometricos_db?retryWrites=true&w=majority&appName=AppCluster";

mongoose.connect(mongoUri)
  .then(() => console.log("Conectado a MongoDB Atlas"))
  .catch(err => console.error("Error conectando a MongoDB:", err));

// Esquema para registros_bitacora
const trainingSchema = new mongoose.Schema({
  username: String,
  rawText: String,
  distanceKm: Number,
  durationMin: Number,
  timestamp: Number
}, { collection: 'registros_bitacora' });

const Training = mongoose.model('Training', trainingSchema);

// Esquema para datos_biometricos
const biometricLogSchema = new mongoose.Schema({
  username: String,
  success: Boolean,
  timestamp: Number
}, { collection: 'datos_biometricos' });

const BiometricLog = mongoose.model('BiometricLog', biometricLogSchema);

// Rutas para Entrenamientos
app.post('/trainings', async (req, res) => {
  try {
    const training = new Training(req.body);
    await training.save();
    res.status(201).send(training);
  } catch (error) {
    res.status(400).send(error);
  }
});

app.get('/trainings/:username', async (req, res) => {
  try {
    const trainings = await Training.find({ username: req.params.username }).sort({ timestamp: -1 });
    res.send(trainings);
  } catch (error) {
    res.status(500).send(error);
  }
});

// Rutas para Biometría
app.post('/biometric-logs', async (req, res) => {
  try {
    const log = new BiometricLog(req.body);
    await log.save();
    res.status(201).send(log);
  } catch (error) {
    res.status(400).send(error);
  }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  console.log(`Servidor corriendo en el puerto ${PORT}`);
});
