export function UploadBox({
  selectedFile,
  setSelectedFile,
  fileInputRef,
  onUpload,
  loading
}) {

  return (

    <section className="upload-box">

      <form 
        className="upload-content"
        onSubmit={onUpload}
      >

        <div className="upload-icon">
          📂
        </div>


        <h3>
          Subir archivo
        </h3>


        <p>
          Selecciona un archivo para almacenarlo.
        </p>



        <label className="upload-zone">


          <span>
            {
              selectedFile
              ? selectedFile.name
              : "Selecciona un archivo"
            }
          </span>


          <input

            ref={fileInputRef}

            type="file"

            onChange={(event)=>{

              setSelectedFile(
                event.target.files?.[0] || null
              )

            }}

          />

        </label>



        <button

          type="submit"

          className="primary-btn"

          disabled={loading}

        >

          {
            loading
            ? "Subiendo..."
            : "Subir archivo"
          }

        </button>


      </form>

    </section>

  )
}