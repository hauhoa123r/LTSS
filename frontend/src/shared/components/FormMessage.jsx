function FormMessage({ error, success }) {
  if (error) {
    return (
      <div className="form-message form-message--error" role="alert">
        <strong>{error.message}</strong>
        {error.requestId && <small>Mã yêu cầu: {error.requestId}</small>}
      </div>
    )
  }
  if (success) {
    return <div className="form-message form-message--success">{success}</div>
  }
  return null
}

export default FormMessage
