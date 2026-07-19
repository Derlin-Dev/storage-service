import { FileCard } from './FileCard.jsx'


export function FileGrid({
  files,
  onShare,
  onDownload,
  onDelete
}) {


  if(files.length === 0){

    return (

      <div className="empty-state">

        Aún no tienes archivos.

      </div>

    )

  }



  return (

    <section className="file-grid">


      {
        files.map(file=>(

          <FileCard

            key={file.id}

            file={file}

            onShare={onShare}

            onDownload={onDownload}

            onDelete={onDelete}

          />

        ))
      }


    </section>

  )

}