post '/send/:who' do
    halt 401 unless secure_eq?(request.env['HTTP_AUTHORIZATION'], "Bearer #{ENV.fetch('SMS_TOKEN')}")
    number = CONTACTS[params[:who]] or halt 404, "unknown contact\n"
    text = request.body.read.strip
    halt 400, "empty body\n" if text.empty?
    ok, out = send_sms(number, text)
    ok ? "sent\n" : halt(502, out)
end