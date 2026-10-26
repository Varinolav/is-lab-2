import groovy.json.JsonOutput
import groovy.json.JsonSlurperClassic
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.nio.charset.StandardCharsets
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.TimeUnit

// Two JMeter threads = two independent HTTP sessions. No JMeter plugins required.
def stage = args[0]
def user = ctx.getThreadNum()
def iteration = vars.getIteration()
def run = props.getProperty('runId', 'manual')
def prefix = 'JM_' + run + '_'
def base = props.getProperty('baseUrl', 'http://localhost:8080').replaceAll('/+$', '') + '/api'
def timeout = Integer.parseInt(props.getProperty('timeoutSeconds', '30'))
def shared
synchronized (props) {
    def key = 'lab2.jmeter.' + run
    if (!props.containsKey(key)) {
        props.put(key, [barrier: new CyclicBarrier(2), data: new ConcurrentHashMap(),
                        owned: new ConcurrentLinkedQueue()])
    }
    shared = props.get(key)
}
def evidence = { Map record ->
    def file = new File(props.getProperty('evidenceFile', 'jmeter-evidence.jsonl'))
    synchronized (shared) {
        file.append(JsonOutput.toJson([run: run, iteration: iteration, user: user,
                                      stage: stage, timestamp: System.currentTimeMillis()] + record) + '\n', 'UTF-8')
    }
}
def check = { boolean condition, String message ->
    if (!condition) throw new IllegalStateException(message)
}
def client = vars.getObject('httpClient')
if (client == null) {
    client = HttpClient.newBuilder().cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL))
            .connectTimeout(Duration.ofSeconds(timeout)).build()
    vars.putObject('httpClient', client)
}
def request = { String method, String path, Object body = null, boolean multipart = false ->
    def builder = HttpRequest.newBuilder(URI.create(base + path)).timeout(Duration.ofSeconds(timeout))
            .header('Accept', 'application/json')
    String encoded = ''
    if (body != null) {
        if (multipart) {
            def boundary = 'JMeterBoundary' + UUID.randomUUID().toString().replace('-', '')
            builder.header('Content-Type', 'multipart/form-data; boundary=' + boundary)
            encoded = '--' + boundary + '\r\nContent-Disposition: form-data; name="file"; filename="movies.json"' +
                    '\r\nContent-Type: application/json\r\n\r\n' + JsonOutput.toJson(body) +
                    '\r\n--' + boundary + '--\r\n'
        } else {
            builder.header('Content-Type', 'application/json')
            encoded = JsonOutput.toJson(body)
        }
    }
    builder.method(method, body == null ? HttpRequest.BodyPublishers.noBody() :
            HttpRequest.BodyPublishers.ofString(encoded, StandardCharsets.UTF_8))
    def start = System.nanoTime()
    def response = client.send(builder.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8))
    def text = response.body()
    def json = null
    // Eager nested maps: the shared fixture is read concurrently by both users.
    try { if (text) json = new JsonSlurperClassic().parseText(text) } catch (Exception ignored) { }
    def result = [code: response.statusCode(), json: json, text: text]
    evidence([method: method, path: path, status: result.code,
              elapsedMs: (System.nanoTime() - start) / 1000000,
              response: path.startsWith('/auth') ? '(authentication response omitted)' : text.take(3000)])
    result
}
def expect = { Map response, List codes ->
    check(codes.contains(response.code), "Expected HTTP ${codes}, got ${response.code}: ${response.text.take(700)}")
    response.json
}
// Optional client retries for ordinary creates/imports, never for competing race requests.
// A serialization-conflict response means that the entire transaction was rolled back.
def ordinary = { String method, String path, Object body = null, boolean multipart = false ->
    def response
    int attempts = props.getProperty('retryConflicts', 'false') == 'true' ? 4 : 1
    for (int attempt = 0; attempt < attempts; attempt++) {
        response = request(method, path, body, multipart)
        if (response.code != 409 || response.json?.code != 'TRANSACTION_CONFLICT') break
        if (attempt + 1 < attempts) Thread.sleep(30L * (attempt + 1) + user * 17L)
    }
    response
}
def own = { String type, Object id -> if (id != null) shared.owned.add([type: type, id: id]) }
def create = { String type, Map body ->
    def obj = expect(ordinary('POST', '/' + type, body), [201])
    own(type, obj.id)
    obj
}
def movie = { String name ->
    def refs = vars.getObject('refs')
    [name: name, coordinates: refs.coordinates, director: refs.person, operator: refs.person,
     oscarsCount: 0, mpaaRating: 'PG_13', budget: 100, length: 90, genre: 'DRAMA']
}
def imported = { String name ->
    [name: name, coordinates: [x: 10, y: 20], oscarsCount: 0, mpaaRating: 'PG_13',
     director: [name: name + '_director', location: [x: 1, y: 2, z: 3]],
     operator: [name: name + '_operator', location: [x: 4, y: 5, z: 6]]]
}
def listMovies = { -> expect(request('GET', '/movies'), [200]) }
def named = { String name -> listMovies().findAll { it.name.strip().toLowerCase(Locale.ROOT) == name.strip().toLowerCase(Locale.ROOT) } }
def await = { -> shared.barrier.await(timeout * 3L, TimeUnit.SECONDS) }
// Every participant traverses the same barriers even when setup/action/assertions fail.
def pair = { Closure setup, Closure action, Closure verify ->
    await()
    if (user == 0) {
        shared.data.clear()
        try { shared.data.put('fixture', setup() ?: [:]) }
        catch (Exception e) { shared.data.put('setupError', e.toString()) }
    }
    await()
    if (!shared.data.containsKey('setupError')) {
        try { shared.data.put('result' + user, action(shared.data.fixture)) }
        catch (Exception e) { shared.data.put('result' + user, [error: e.toString()]) }
    }
    await()
    if (user == 0 && !shared.data.containsKey('setupError')) {
        try {
            def results = [shared.data.result0, shared.data.result1]
            check(results.every { it != null && !it.containsKey('error') }, 'Request error: ' + results)
            verify(shared.data.fixture, results)
        } catch (Exception e) { shared.data.put('checkError', e.toString()) }
    }
    await()
    check(!shared.data.containsKey('setupError'), 'Setup failed: ' + shared.data.setupError)
    check(!shared.data.containsKey('checkError'), 'Concurrent scenario failed: ' + shared.data.checkError)
}
def name = prefix + iteration + '_' + stage
try {
    switch (stage) {
        case 'bootstrap':
            if (iteration == 1) {
                // Account registration is setup, not a concurrency scenario under study.
                // Create accounts sequentially so SSI conflicts in AuthService cannot invalidate the experiment.
                pair({
                    def accounts = (0..1).collect { index ->
                        [username: prefix + 'admin' + index, password: UUID.randomUUID().toString(), role: 'ADMIN']
                    }
                    accounts.each { expect(request('POST', '/auth/register', it), [201]) }
                    [accounts: accounts]
                }, { fixture ->
                    def account = fixture.accounts[user]
                    expect(request('POST', '/auth/login', [username: account.username, password: account.password]), [200])
                    def me = expect(request('GET', '/auth/me'), [200])
                    check(me.username == account.username && me.role == 'ADMIN', 'Wrong user or role: ' + me)
                    [code: 200]
                }, { fixture, results -> })
            }
            def location = create('locations', [x: 1, y: 2, z: 3, name: name + '_' + user])
            def person = create('persons', [name: name + '_person_' + user, location: location])
            def coordinates = create('coordinates', [x: 10, y: 20])
            vars.putObject('refs', [person: person, coordinates: coordinates])
            break
        case 'crud':
            def obj = create('movies', movie(name + '_' + user))
            def updated = movie(obj.name + '_edited') + [budget: 101]
            expect(ordinary('PUT', '/movies/' + obj.id, updated), [200])
            def saved = expect(request('GET', '/movies/' + obj.id), [200])
            check(saved.name == updated.name && saved.budget == 101, 'CRUD update not saved')
            expect(ordinary('DELETE', '/movies/' + obj.id), [204])
            expect(request('GET', '/movies/' + obj.id), [404])
            break
        case 'update-update':
            pair({ create('movies', movie(name)) }, { obj ->
                // Both clients start from the same snapshot, and change different fields.
                request('PUT', '/movies/' + obj.id, obj + (user == 0 ? [budget: 101] : [length: 91]))
            }, { obj, results ->
                results.each { expect(it, [200, 409, 412]) }
                check(results.count { it.code == 200 } >= 1, 'No successful update')
                def saved = expect(request('GET', '/movies/' + obj.id), [200])
                if (results.every { it.code == 200 }) {
                    check(saved.budget == 101 && saved.length == 91,
                            "LOST UPDATE: both PUTs returned 200; final budget=${saved.budget}, length=${saved.length}")
                }
            })
            break
        case 'update-delete':
            pair({ create('movies', movie(name)) }, { obj ->
                user == 0 ? request('PUT', '/movies/' + obj.id, obj + [budget: 102]) :
                        request('DELETE', '/movies/' + obj.id)
            }, { obj, results ->
                expect(results[0], [200, 404, 409, 412])
                expect(results[1], [204])
                expect(request('GET', '/movies/' + obj.id), [404])
                check(named(name).isEmpty(), 'Deleted movie was recreated under another id')
            })
            break
        case 'delete-delete':
            pair({ create('movies', movie(name)) }, { obj -> request('DELETE', '/movies/' + obj.id) }, { obj, results ->
                def codes = results.collect { it.code }
                check(codes.count(204) == 1 && codes.every { it in [204, 404, 409] },
                        'Expected one 204 and one 404/409: ' + codes)
                expect(request('GET', '/movies/' + obj.id), [404])
            })
            break
        case 'unique-create':
            pair({ [name: name] }, { fixture ->
                def response = request('POST', '/movies', movie(user == 0 ? fixture.name : '  ' + fixture.name.toLowerCase(Locale.ROOT) + '  '))
                if (response.code == 201) own('movies', response.json.id)
                response
            }, { fixture, results ->
                def matches = named(fixture.name)
                evidence([invariant: 'normalized-name-unique', matches: matches.size(), codes: results*.code])
                check(results.collect { it.code }.sort() == [201, 409], 'Uniqueness: expected 201/409, got ' + results*.code)
                check(matches.size() == 1, 'Duplicate normalized movie names')
            })
            break
        case 'valid-import':
            def batch = [imported(name + '_' + user + '_a'), imported(name + '_' + user + '_b')]
            def response = expect(ordinary('POST', '/movies/import', batch, true), [201])
            check(response.importedCount == 2, 'Wrong importedCount: ' + response)
            batch.each { check(named(it.name).size() == 1, 'Imported movie missing or duplicated: ' + it.name) }
            break
        case 'unique-import':
            pair({ [name: name] }, { fixture ->
                request('POST', '/movies/import', [imported(fixture.name), imported(fixture.name + '_tail_' + user)], true)
            }, { fixture, results ->
                def matches = named(fixture.name)
                evidence([invariant: 'import-name-unique', matches: matches.size(), codes: results*.code])
                check(results.collect { it.code }.sort() == [201, 409], 'Import race: expected 201/409, got ' + results*.code)
                check(matches.size() == 1, 'Duplicate name after concurrent imports')
                results.eachWithIndex { response, index ->
                    check(named(fixture.name + '_tail_' + index).size() == (response.code == 201 ? 1 : 0),
                            'Failed import partially committed, or successful import incomplete')
                }
            })
            break
        case 'create-import':
            pair({ [name: name] }, { fixture ->
                user == 0 ? request('POST', '/movies', movie(fixture.name)) :
                        request('POST', '/movies/import', [imported(fixture.name), imported(fixture.name + '_tail')], true)
            }, { fixture, results ->
                def matches = named(fixture.name)
                evidence([invariant: 'create-import-name-unique', matches: matches.size(), codes: results*.code])
                check(results.collect { it.code }.sort() == [201, 409], 'Create/import: expected 201/409, got ' + results*.code)
                check(matches.size() == 1, 'Duplicate name after create/import')
                check(named(fixture.name + '_tail').size() == (results[1].code == 201 ? 1 : 0), 'Import atomicity violated')
            })
            break
        case 'invalid-import':
            def batch = [imported(name + '_' + user + '_valid'), imported(name + '_' + user + '_invalid')]
            batch[1].director.remove('location')
            expect(request('POST', '/movies/import', batch, true), [400])
            batch.each { check(named(it.name).isEmpty(), 'Invalid import left a movie in the database') }
            def history = expect(request('GET', '/imports'), [200])
            def username = prefix + 'admin' + user
            check(history.any { it.username == username && it.status == 'SUCCESS' && it.count == 2 }, 'Successful import history missing')
            check(history.any { it.username == username && it.status == 'FAILED' }, 'Failed import history missing')
            break
        case 'cleanup':
            pair({ [:] }, { ignored -> [code: 200] }, { ignored, results ->
                def prefixes = [prefix]
                // Recover only records proven to belong to an explicitly named previous run.
                if (iteration == 1) {
                    props.getProperty('cleanupRunIds', '').split(',').findAll { it }.each { oldRun ->
                        check(oldRun ==~ /[a-zA-Z0-9_-]+/, 'Invalid cleanup run id')
                        def oldEvidence = new File(props.getProperty('suiteDir'), 'results/' + oldRun + '/evidence.jsonl')
                        check(oldEvidence.isFile(), 'Cleanup evidence missing: ' + oldRun)
                        prefixes.add('JM_' + oldRun + '_')
                        oldEvidence.eachLine('UTF-8') { line ->
                            def record = new JsonSlurperClassic().parseText(line)
                            if (record.run == oldRun && record.method == 'POST' && record.status == 201 &&
                                    record.path in ['/movies', '/persons', '/coordinates', '/locations']) {
                                def object = new JsonSlurperClassic().parseText(record.response)
                                own(record.path.substring(1), object.id)
                            }
                        }
                    }
                }
                def belongsToRun = { obj -> prefixes.any { obj.name.toLowerCase(Locale.ROOT).startsWith(it.toLowerCase(Locale.ROOT)) } }
                // Discover nested objects from our imports before deleting their parent movies.
                def ours = listMovies().findAll(belongsToRun)
                ours.each { obj ->
                    own('movies', obj.id)
                    own('coordinates', obj.coordinates.id)
                    [obj.director, obj.operator, obj.screenwriter].findAll { it != null }.each { person ->
                        own('persons', person.id)
                        own('locations', person.location.id)
                    }
                }
                ['movies', 'persons', 'coordinates', 'locations'].each { type ->
                    shared.owned.findAll { it.type == type }.collect { it.id }.unique().each { id ->
                        expect(request('DELETE', '/' + type + '/' + id), [204, 404])
                    }
                }
                shared.owned.clear()
                check(listMovies().every { !belongsToRun(it) }, 'Test movies remain after cleanup')
            })
            break
        default: throw new IllegalArgumentException('Unknown stage: ' + stage)
    }
    SampleResult.setSuccessful(true)
    SampleResult.setResponseCode('200')
    SampleResult.setResponseMessage('Scenario assertions passed')
    SampleResult.setResponseData(JsonOutput.toJson([stage: stage, user: user, iteration: iteration, passed: true]), 'UTF-8')
    evidence([scenarioPassed: true])
} catch (Exception e) {
    SampleResult.setSuccessful(false)
    SampleResult.setResponseCode('ASSERTION_FAILED')
    SampleResult.setResponseMessage(e.toString().take(1500))
    SampleResult.setResponseData(e.toString(), 'UTF-8')
    evidence([scenarioPassed: false, error: e.toString()])
    log.error('Scenario {} failed for user {}', stage, user, e)
}
