function formatBytes(size) {

  if (!size) return "0 B"

  const units = [
    "B",
    "KB",
    "MB",
    "GB"
  ]

  const index =
    Math.min(
      Math.floor(
        Math.log(size) /
        Math.log(1024)
      ),
      units.length - 1
    )


  const value =
    size /
    1024 ** index


  return `${value.toFixed(index === 0 ? 0 : 1)} ${units[index]}`

}



export function FileCard({
  file,
  onShare,
  onDownload,
  onDelete
}) {


  return (

    <article className="file-card">


      <div className="file-icon">
        📄
      </div>


      <div className="file-info">


        <h3>
          {file.originalName}
        </h3>


        <p>
          {file.contentType}
          {" · "}
          {formatBytes(file.size)}
        </p>


        <small>
          {
            file.createdAt?.slice(0,10)
            || "—"
          }
        </small>


      </div>



      <div className="file-actions">


        <button
          className="secondary-btn"
          onClick={()=>onShare(file)}
        >
          Compartir
        </button>


        <button
          className="secondary-btn"
          onClick={()=>onDownload(file)}
        >
          Descargar
        </button>


        <button
          className="danger-btn"
          onClick={()=>onDelete(file)}
        >
          Eliminar
        </button>


      </div>


    </article>

  )

}