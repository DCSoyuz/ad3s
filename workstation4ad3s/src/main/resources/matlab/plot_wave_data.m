%% plot_wave_data.m
%  Парсер и отрисовщик файлов wave_data из HandTap Workstation4ad3s.
%
%  Использование:
%    plot_wave_data('wave_addr1_20260615_143022.txt')
%    plot_wave_data()          % откроет диалог выбора файла
%
%  Формат файла:
%    # HandTap data
%    # Addr: <N>  H<1|2>  Trig: <true|false>  Threshold: <N>
%    Field1<TAB>Field2<TAB>...<TAB>FieldN
%    VAL1<TAB>VAL2<TAB>...<TAB>VALN     (hex %07X, одна строка = один отсчёт)

function plot_wave_data(filename)
    %% Выбор файла
    if nargin < 1 || isempty(filename)
        [fname, fpath] = uigetfile({'*.txt', 'Wave data files (*.txt)'}, 'Select wave data file');
        if isequal(fname, 0), return; end
        filename = fullfile(fpath, fname);
    end

    %% Чтение файла
    fileLines = readlines(filename, 'EmptyLineRule', 'skip');

    if isempty(fileLines)
        error('File is empty: %s', filename);
    end

    %% Парсинг заголовков
    addrStr  = '';
    handSel  = 'H1';
    trigSel  = false;
    threshVal = 0;
    fieldNames = {};
    dataStartIdx = 1;

    for i = 1:numel(fileLines)
        line = strtrim(fileLines{i});
        if isempty(line), continue; end

        % Комментарии с параметрами
        if startsWith(line, '# Addr:')
            % Формат: # Addr: 1  H1  Trig: true  Threshold: 0
            tokens = regexp(line, '# Addr:\s*(\d+)\s+H(\d)\s+Trig:\s*(\w+)\s+Threshold:\s*(\d+)', 'tokens');
            if ~isempty(tokens)
                addrStr   = tokens{1}{1};
                handSel   = sprintf('H%s', tokens{1}{2});
                trigSel   = strcmpi(tokens{1}{3}, 'true');
                threshVal = str2double(tokens{1}{4});
            end
            dataStartIdx = i + 1;
            continue;
        end

        if startsWith(line, '#')
            dataStartIdx = i + 1;
            continue;
        end

        % Строка с именами полей (содержит табы)
        if isempty(fieldNames) && contains(line, char(9))
            % Проверяем, что это действительно строка заголовков, а не данных
            % Заголовок содержит только буквенные символы и подчёркивания
            parts = split(line, char(9));
            isHeader = true;
            for p = 1:numel(parts)
                part = strtrim(parts{p});
                if isempty(part), continue; end
                if ~all(isstrprop(part, 'alphanum') | part == '_')
                    isHeader = false;
                    break;
                end
            end
            if isHeader
                fieldNames = parts;
                dataStartIdx = i + 1;
                continue;
            end
        end

        % Если дошли сюда — это либо заголовок без табов, либо данные
        if isempty(fieldNames)
            parts = split(line, char(9));
            fieldNames = parts;
            dataStartIdx = i + 1;
        end
    end

    %% Чтение данных
    dataLines = fileLines(dataStartIdx:end);
    dataLines = dataLines(~startsWith(strtrim(dataLines), '#'));
    dataLines(dataLines == "") = [];

    if isempty(dataLines) || isempty(fieldNames)
        error('No data or field names found in file: %s', filename);
    end

    numFields = numel(fieldNames);
    numSamples = numel(dataLines);
    data = zeros(numSamples, numFields);

    for r = 1:numSamples
        line = dataLines{r};
        line = strrep(line, char(13), '');  % remove CR (\r) from Windows line endings
        parts = split(strtrim(line), char(9));
        for c = 1:min(numel(parts), numFields)
            val = strtrim(parts{c});
            if ~isempty(val) && all(ismember(val, ['0':'9' 'A':'F' 'a':'f']))
                % Keep only lowest 7 hex digits (28 bits), convert as signed
                if length(val) > 7
                    val = val(end-6:end);
                end
                v = hex2dec(val);
                if v >= 2^27   % bit 27 set → negative 28-bit value
                    v = v - 2^28;
                end
                data(r, c) = v;
            end
        end
    end

    %% Определение: какие поля числовые, какие однобитовые
    % 1-bit signals are stored with offset (+2, +4, +6, +8) so max-min == 1
    numFields_plot = zeros(1, numFields);
    sigFields = false(1, numFields);
    for c = 1:numFields
        colRange = max(data(:, c)) - min(data(:, c));
        if colRange <= 1
            sigFields(c) = 1;
        else
            numFields_plot(c) = 1;
        end
    end

    % Для 1-bit сигналов: снимаем смещение (оставляем 0/1), затем +2 для отображения
    for c = 1:numFields
        if sigFields(c)
            data(:, c) = data(:, c) - min(data(:, c)) + 2;
        end
    end

    %% Построение графиков
    x = 0:(numSamples - 1);
    colorSet = lines(max(numFields, 2));

    title_str = sprintf('HandTap   Addr=%s   %s   Trig=%d   Threshold=%d   Samples=%d', ...
        addrStr, handSel, trigSel, threshVal, numSamples);

    numIdx = find(numFields_plot);
    sigIdx = find(sigFields);

    fig = figure('Name', title_str, 'NumberTitle', 'off', 'Color', 'w', ...
        'Position', [100, 100, 1000, 550]);
    sgtitle(title_str, 'FontSize', 13, 'FontWeight', 'bold');

    if ~isempty(numIdx)
        ax1 = subplot(2, 1, 1);
        hold on;
        for k = 1:numel(numIdx)
            c = numIdx(k);
            plot(x, data(:, c), 'LineWidth', 1.2, 'Color', colorSet(k, :), ...
                'DisplayName', fieldNames{c}, 'Marker', 'none');
        end
        hold off;
        grid on;
        legend('Location', 'eastoutside', 'FontSize', 9);
        ylabel('Value', 'FontSize', 10);
        title('Values', 'FontSize', 11, 'FontWeight', 'bold');
        set(ax1, 'FontSize', 9);
    end

    if ~isempty(sigIdx)
        ax2 = subplot(2, 1, 2);
        hold on;
        for k = 1:numel(sigIdx)
            c = sigIdx(k);
            plot(x, data(:, c), 'LineWidth', 1.2, 'Color', colorSet(k, :), ...
                'DisplayName', fieldNames{c}, 'Marker', 'none');
        end
        hold off;
        grid on;
        ylim([0, 4]);
        set(gca, 'YTick', [0 1 2 3 4]);
        set(gca, 'YTickLabel', {'0' '' '1' '' '2'});
        legend('Location', 'eastoutside', 'FontSize', 9);
        ylabel('Signal', 'FontSize', 10);
        title('Signals', 'FontSize', 11, 'FontWeight', 'bold');
        set(ax2, 'FontSize', 9);
    end

    xlabel('Sample #', 'FontSize', 10);
