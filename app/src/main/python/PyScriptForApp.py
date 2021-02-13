#%% [markdown]
# ## Python Script that the application will call to take a voice clip and manipulate it for TensorFlowLite

# %%
# Test a single clip with tflite model

# tflitePath = 'R:\School\Masters\Thesis\\automated-spoken-language-detection\source\model.tflite'
# filePath = 'R:\School\Masters\Thesis\\automated-spoken-language-detection\database\cv-corpus-6.0-2020-12-11\oneTimeTestDatabase\s19.wav'
def pythonClassify(filePath, tflitePath):
    import numpy as np
    from tensorflow.keras.preprocessing.image import img_to_array
    import tensorflow as tf
    import librosa
    from os.path import dirname, join

    langDBs = ["English", "French", "Arabic", "German", "Persian", "Russian", "Chinese (China)"]

    #tflitePath = join(dirname(__file__), "model/model.tflite")

    interpreter = tf.lite.Interpreter(model_path=tflitePath)
    interpreter.allocate_tensors()

    loadedClip = librosa.load(filePath, 16000)
    # Fix Duration to 10 seconds
    fixedClip = np.concatenate([loadedClip[0]]*9, axis=0)
    fixedClip = fixedClip[0:(16000*10)]
    # Make Spectogram
    hoplength = fixedClip.shape[0] // 500
    spec = librosa.feature.melspectrogram(fixedClip, n_mels=128, hop_length=int(hoplength))
    image = librosa.core.power_to_db(spec)
    image_np = np.asmatrix(image)
    image_np_scaled_temp = (image_np - np.min(image_np))
    image_np_scaled = image_np_scaled_temp / np.max(image_np_scaled_temp)
    spectro = image_np_scaled[:, 0:500]

    input_array = img_to_array((spectro * 255.).astype(np.uint8))
    input_array = np.array([input_array])
    input_array = input_array.astype('float32') / 255.

    input_details = interpreter.get_input_details()
    output_details = interpreter.get_output_details()
    interpreter.set_tensor(input_details[0]['index'], input_array)
    interpreter.invoke()
    result = interpreter.get_tensor(output_details[0]['index'])

    classification = np.max(result[0])
    classificationOfLanguage = langDBs[np.where(result[0] == classification)[0][0]]
    returnStatement = "I am  " + str(round(classification*100, 2)) + "% sure that this is " +  classificationOfLanguage
    return returnStatement

def pythonWarmUpModel(tflitePath):
    import tensorflow as tf

    interpreter = tf.lite.Interpreter(model_path=tflitePath)
    interpreter.allocate_tensors()

# %%
